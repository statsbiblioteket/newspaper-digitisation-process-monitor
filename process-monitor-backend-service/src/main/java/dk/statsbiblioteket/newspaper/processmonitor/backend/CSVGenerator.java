package dk.statsbiblioteket.newspaper.processmonitor.backend;

import csv.TableWriter;
import csv.impl.CSVWriter;
import csv.impl.type.DateConversionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Class that generates a CSV blob based on batch events.
 */
@Provider
@Produces("text/csv")
public class CSVGenerator implements MessageBodyWriter<Object> {
    Logger log = LoggerFactory.getLogger(getClass());

    /** List and order of events to present in CSV files in different columns */
    private final List<String> EVENTS = Arrays.asList(
            //Note: When this list is updated you must also always update the list in index.html
            "Manually_stopped",
            "Shipped_to_supplier",
            "Data_Received",
            "Metadata_Archived",
            "Data_Archived",
            "Structure_Checked",
            "JPylyzed",
            "Histogrammed",
            "Metadata_checked",
            "Manual_QA_Flagged",
            "Roundtrip_Approved",
            "Dissemination_Copy_Generated",
            "Dissemination_Editions_Generated",
            "Metadata_Enriched",
            "Cleaned_lesser_roundtrips",
            "Data_Released",
            "Received_from_supplier");

    /** How many columns are used per event */
    private final int COLUMNS_PER_EVENT = 3;
    /** How many columns are used per row for headers */
    private final int ROW_HEADER_COLUMNS = 7;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Batch.class.isAssignableFrom(type)
                || Event.class.isAssignableFrom(type)
                || (List.class.isAssignableFrom(type));
    }

    @Override
    public long getSize(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        if (Batch.class.isAssignableFrom(o.getClass())) {
            entityStream.write(generateCSV((Batch) o).getBytes());
        } else if (Event.class.isAssignableFrom(o.getClass())) {
            entityStream.write(generateCSV((Event) o).getBytes());
        } else if (List.class.isAssignableFrom(o.getClass())) {
            entityStream.write(generateCSV((List<Batch>) o).getBytes());
        } else {
            log.warn("Unknown object type {} in CSV Generator", o.getClass());
            throw new WebApplicationException(Response.Status.NOT_ACCEPTABLE);
        }
    }

    /**
     * Generate CSV blob for a list of batches and events happened on these.
     * The report will contain a header row, and a row per batch with events for that batch.
     *
     * @param batches The batches to generate CSV for.
     * @return A CSV Blob
     * @throws IOException If the blob cannot be generated.
     */
    private String generateCSV(List<Batch> batches)
            throws IOException {
        log.debug("Generating CSV for List<Batch>: {}", batches);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        TableWriter csvWriter = getTableWriter(stream);
        printHeader(csvWriter);
        for (Batch batch : batches) {
            generateCSVForBatch(csvWriter, batch);
        }
        return stream.toString("UTF-8");
    }

    /**
     * Generate CSV blob for a single batch and events happened on this.
     * The report will contain a header row and a single row with events for the batch.
     *
     * @param batch The batch to generate CSV for.
     * @return A CSV Blob
     * @throws IOException If the blob cannot be generated.
     */
    private String generateCSV(Batch batch) throws IOException {
        log.debug("Generating CSV for Batch: {}", batch.getBatchID());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        TableWriter csvWriter = getTableWriter(stream);
        printHeader(csvWriter);
        generateCSVForBatch(csvWriter, batch);
        return stream.toString("UTF-8");
    }

    /**
     * Generate CSV blob for a single batch event.
     * The report will contain a single row, with information about the event.
     *
     * @param event The event to generate CSV for.
     * @return A CSV Blob
     * @throws IOException If the blob cannot be generated.
     */
    private String generateCSV(Event event) throws IOException {
        log.debug("Generating CSV for Event: {}, {}, {}", event.isSuccess(), event.getDate(), event.getDetails());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        TableWriter csvWriter = getTableWriter(stream);
        Object[] row = new Object[COLUMNS_PER_EVENT + ROW_HEADER_COLUMNS];
        // Print the event details.
        generateCSVForEvent(EVENTS.get(0), event, row);
        csvWriter.printRow(row);
        return stream.toString("UTF-8");
    }

    /**
     * Get a suitable table writer for a given output stream.
     * @param stream The stream to get a table writer for.
     * @return The table writer.
     */
    private TableWriter getTableWriter(OutputStream stream) {
        CSVWriter csvWriter = new CSVWriter(stream);
        DateConversionHandler handler = new DateConversionHandler();
        handler.setPrintFormat("yyyy-MM-dd HH:mm:ss");
        csvWriter.registerTypeConversionHandler(handler);
        return csvWriter;
    }

    /**
     * Generate a header matching the CSV rows for all events on a batch.
     *
     * @param csvWriter The csvWriter to generate the header on.
     * @throws IOException If the header row cannot be generated.
     */
    private void printHeader(TableWriter csvWriter) throws IOException {
        // A row with the right length.
        Object[] header = new Object[EVENTS.size() * COLUMNS_PER_EVENT + ROW_HEADER_COLUMNS];

        // Row headers
        updateCell(header, 0, "Batch");
        updateCell(header, 1, "Roundtrip");
        updateCell(header, 2, "Avis id");
        updateCell(header, 3, "Start date");
        updateCell(header, 4, "End date");
        updateCell(header, 5, "Pages");
        updateCell(header, 6, "Unmatched Pages");

        // Event headers
        int index = ROW_HEADER_COLUMNS;
        for (String event : EVENTS) {
            updateCell(header, index, event);
            updateCell(header, index+1, event + "_timestamp");
            updateCell(header, index+2, event + "_duration");
            index += COLUMNS_PER_EVENT;
        }
        csvWriter.printRow(header);
    }

    /**
     * Run through all events for a batch, and generate a row containing information about events for that batch.
     *
     * @param csvWriter The csvWriter to generate the row on.
     * @param batch The batch to generate a row for.
     * @throws IOException If the row cannot be generated.
     */
    private void generateCSVForBatch(TableWriter csvWriter, Batch batch) throws IOException {
        // A row with the right length.
        Object[] row = new Object[EVENTS.size() * COLUMNS_PER_EVENT + ROW_HEADER_COLUMNS];

        // Row headers
        updateCell(row, 0, "=\"" + batch.getBatchID() + "\"");
        updateCell(row, 1, batch.getRoundTripNumber());
        updateCell(row, 2, batch.getAvisID());
        updateCell(row, 3, batch.getStartDate());
        updateCell(row, 4, batch.getEndDate());
        updateCell(row, 5, batch.getNumberOfPages());
        updateCell(row, 6, batch.getNumberOfUnmatched());

        // Events
        Map<String, Event> events = batch.getEvents();
        for (Map.Entry<String, Event> event : events.entrySet()) {
            // Fill out details for the event.
            generateCSVForEvent(event.getKey(), event.getValue(), row);
        }
        csvWriter.printRow(row);
    }

    /**
     * Given an event, fill out the cells of the row with information about that event.
     *
     * @param eventID The ID of the event.
     * @param event The event to process.
     * @param row The row to fill out cells in.
     */
    private void generateCSVForEvent(String eventID, Event event, Object[] row) {
        // Find the index of the event, to fill out the right cells.
        int index = EVENTS.indexOf(eventID);
        if (index == -1) {
            // Unknown events are not included in the report
            return;
        }
        updateCell(row, index * COLUMNS_PER_EVENT + ROW_HEADER_COLUMNS, event.isSuccess());
        updateCell(row, index * COLUMNS_PER_EVENT + ROW_HEADER_COLUMNS + 1, event.getDate() != null ? new Date(event.getDate().getTime()) : null);
        updateCell(row, index * COLUMNS_PER_EVENT + ROW_HEADER_COLUMNS + 2, event.getDuration());
    }

    /**
     * Update a cell in a row. If the cell is empty, insert the value. Otherwise add the value, by updating it to a
     * comma-separated list of values.
     *
     * @param row The row to update.
     * @param index The index of the cell in the row to update.
     * @param value The value to update the cell with.
     */
    private void updateCell(Object[] row, int index, Object value) {
        if (row[index] == null) {
            row[index] = value;
        } else {
            row[index] = row[index].toString() + "," + value.toString();
        }
    }
}
