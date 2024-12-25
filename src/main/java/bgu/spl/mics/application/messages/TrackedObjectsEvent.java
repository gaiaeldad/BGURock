package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.TrackedObject;
import java.util.List;

public class TrackedObjectsEvent implements Event <Void>{
    private final int time; 
    private final List<TrackedObject> trackedObjects; 
    private String senderName;


  
    public TrackedObjectsEvent(int time, List<TrackedObject> trackedObjects, String senderName) {
        this.time = time;
        this.trackedObjects = trackedObjects;
        this.senderName = senderName;
    }
    public int getTime() {
        return time;
    }
    public List<TrackedObject> getTrackedObjects() {
        return trackedObjects;
    }
    public String getSenderName() {
        return senderName;
    }
}


   