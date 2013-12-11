package dk.statsbiblioteket.newspaper.processmonitor.backend;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Test CSV generator methods
 */
public class CSVGeneratorTest {
    private static final Map<String, Event> EVENT_MAP_1;
    static {
        Map<String, Event> eventMap1 = new HashMap<>();
        eventMap1.put("Data_Received", getTestEvent(new Date(0), false, "Test"));
        eventMap1.put("JPylyzed", getTestEvent(new Date(1000), false, "Hello World"));
        eventMap1.put("Unknown", getTestEvent(new Date(2000), false, null));
        EVENT_MAP_1 = eventMap1;
    }

    private static final Map<String, Event> EVENT_MAP_2;
    static {
        Map<String, Event> eventMap2 = new HashMap<>();
        eventMap2.put("Structure_Checked", getTestEvent(new Date(1000), false, "Test"));
        eventMap2.put("Metadata_Archived", getTestEvent(new Date(0), true, null));
        eventMap2.put("Shipped_to_supplier", getTestEvent(new Date(2000), true, "æøå\nabc"));
        EVENT_MAP_2 = eventMap2;
    }

    private static final Batch TEST_BATCH_1 = getTestBatch("4000000000", 2, EVENT_MAP_1);

    private static final Batch TEST_BATCH_2 = getTestBatch("4000000001", 1, EVENT_MAP_2);

    private static final List<Batch> TEST_BATCHES = Arrays.asList(TEST_BATCH_1, TEST_BATCH_2);

    private static final Event TEST_EVENT = getTestEvent(new Date(1000), false, "The\ndetails\næøå");

    @Test
    public void testGenerateCSVForBatchList() throws Exception {
        String expectedOutput
                = "Batch;Roundtrip;Shipped_to_supplier;;;Data_Received;;;Metadata_Archived;;;Data_Archived;;;Structure_Checked;;;JPylyzed;;;Metadata_checked;;;auto-qa;;;manuel-qa;;;Approved;;;Received_from_supplier;;\n"
                + "4000000000;2;;;;false;;Test;;;;;;;;;;false;;Hello World;;;;;;;;;;;;;;;\n"
                + "4000000001;1;true;;\"æøå\nabc\";;;;true;;;;;;false;;Test;;;;;;;;;;;;;;;;;;\n";
        assertEquals(CSVGenerator.generateCSVForBatchList(TEST_BATCHES, true), expectedOutput);
    }

    @Test
    public void testGenerateCSVForBatch() throws Exception {
        String expectedOutput
                = "Batch;Roundtrip;Shipped_to_supplier;;;Data_Received;;;Metadata_Archived;;;Data_Archived;;;Structure_Checked;;;JPylyzed;;;Metadata_checked;;;auto-qa;;;manuel-qa;;;Approved;;;Received_from_supplier;;\n"
                + "4000000000;2;;;;false;;Test;;;;;;;;;;false;;Hello World;;;;;;;;;;;;;;;\n";
        assertEquals(CSVGenerator.generateCSVForBatch(TEST_BATCH_1, true), expectedOutput);
    }

    @Test
    public void testGenerateCSVForBatchNoDetails() throws Exception {
        Batch batch = TEST_BATCH_2;
        String expectedOutput
                = "Batch;Roundtrip;Shipped_to_supplier;Data_Received;Metadata_Archived;Data_Archived;Structure_Checked;JPylyzed;Metadata_checked;auto-qa;manuel-qa;Approved;Received_from_supplier\n"
                + "4000000001;1;true;;true;;false;;;;;;\n";
        assertEquals(CSVGenerator.generateCSVForBatch(batch, false), expectedOutput);
    }

    @Test
    public void testGenerateCSVForEvent() throws Exception {
        String expectedOutput=";;false;;\"The\ndetails\næøå\"\n";
        assertEquals(CSVGenerator.generateCSVForEvent(TEST_EVENT, true), expectedOutput);
    }

    private static Batch getTestBatch(String batchID, int roundTripNumber, Map<String, Event> events) {
        Batch batch = new Batch();
        batch.setBatchID(batchID);
        batch.setRoundTripNumber(roundTripNumber);
        batch.setEvents(events);
        return batch;
    }

    private static Event getTestEvent(Date date, boolean success, String details) {
        Event event = new Event();
        //event1.setDate(date);
        event.setSuccess(success);
        event.setDetails(details);
        return event;
    }
}
