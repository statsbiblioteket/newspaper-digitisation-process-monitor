package dk.statsbiblioteket.newspaper.processmonitor.backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Converter {
    static List<Batch> convertBatchList(List<dk.statsbiblioteket.newspaper.processmonitor.datasources.Batch> batches) {
        if (batches == null) {
            return null;
        }
        List<Batch> result = new ArrayList<>(batches.size());
        for (dk.statsbiblioteket.newspaper.processmonitor.datasources.Batch batch : batches) {
            result.add(convert(batch));
        }
        return result;
    }

    static Batch convert(dk.statsbiblioteket.newspaper.processmonitor.datasources.Batch batch) {
        if (batch == null) {
            return null;
        }
        Batch result = new Batch();
        result.setBatchID(batch.getBatchID());
        result.setEvents(convert(batch.getEventList()));
        return result;
    }

    private static Map<String, Event> convert(List<dk.statsbiblioteket.newspaper.processmonitor.datasources.Event> eventList) {
        if (eventList == null) {
            return null;
        }
        Map<String, Event> result = new HashMap<>(eventList.size());
        for (dk.statsbiblioteket.newspaper.processmonitor.datasources.Event event : eventList) {
            result.put(event.getEventID(), convert(event));
        }
        return result;
    }

    static Event convert(dk.statsbiblioteket.newspaper.processmonitor.datasources.Event batchEvent) {
        if (batchEvent == null) {
            return null;
        }
        Event result = new Event();
        result.setDetails(batchEvent.getDetails());
        result.setSuccess(batchEvent.isSuccess());
        return result;
    }
}