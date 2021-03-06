package dk.statsbiblioteket.newspaper.processmonitor.backend;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dk.statsbiblioteket.newspaper.mfpakintegration.database.InconsistentDatabaseException;
import dk.statsbiblioteket.newspaper.mfpakintegration.database.MfPakDAO;
import dk.statsbiblioteket.newspaper.mfpakintegration.database.NewspaperDateRange;

@Component
public class MfPakBatchEnricher implements BatchEnricher {

    private MfPakDAO mfpak;
    private Logger log = LoggerFactory.getLogger(getClass());
    public MfPakDAO getMfpak() {
        return mfpak;
    }

    public void setMfpak(MfPakDAO mfpak) {
        this.mfpak = mfpak;
    }
    
    /**
     * Enriches the batch objects with 'avisID' and date intervals from the mfpak database.
     * The returned reference to list of batches is the same as the one taken as input. 
     */
    @Override
    public List<Batch> enrich(List<Batch> batches) {
        for(Batch b : batches) {
            enrich(b);
        }
        return batches;
    }

    @Override
    public Batch enrich(Batch batch) {
        String batchID = batch.getBatchID();
        try {
            batch.setAvisID(mfpak.getNewspaperID(batchID));
            enrichWithDateRange(batch);
        } catch (InconsistentDatabaseException | SQLException e) {
            log.warn("Failed to enrich batch {}", batchID, e);
        }
        return batch;
    }

    /**
     * Enriches the batch with the start and stop dates from mfpak 
     */
    private Batch enrichWithDateRange(Batch batch) throws SQLException {
        List<NewspaperDateRange> ranges = mfpak.getBatchDateRanges(batch.getBatchID());
        Date tempStartDate = new Date();
        Date tempEndDate = new Date(Long.MIN_VALUE);
        
        if(ranges != null && !ranges.isEmpty()) {
            for(NewspaperDateRange range : ranges) {
                if(range.getFromDate().before(tempStartDate)) {
                    tempStartDate = range.getFromDate();
                }
                
                if(range.getToDate().after(tempEndDate)) {
                    tempEndDate = range.getToDate();
                }
            }
            
            batch.setStartDate(tempStartDate);
            batch.setEndDate(tempEndDate);
        } else {
            log.debug("No date range found for batch {}, not enriching with range information", batch.getBatchID());
        }
        
        return batch;
    }
}
