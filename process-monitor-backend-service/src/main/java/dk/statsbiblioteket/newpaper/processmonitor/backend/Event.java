package dk.statsbiblioteket.newpaper.processmonitor.backend;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Event {

    private String eventID;
    private boolean success;
    private String details;
    
    public String getEventID() {
        return eventID;
    }
    
    public void setEventID(String eventID) {
        this.eventID = eventID;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean passed) {
        this.success = passed;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
}
