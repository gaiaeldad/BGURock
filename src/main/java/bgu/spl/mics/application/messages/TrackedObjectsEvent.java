package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.TrackedObject;
import java.util.List;

public class TrackedObjectsEvent implements Event <Boolean>{
    private final int foundTime; 
    private final List<TrackedObject> trackedObjects; 
    private String senderName;
    private int designatedTime;
    DetectObjectsEvent handeledEvent;
  
    public TrackedObjectsEvent(DetectObjectsEvent event, int time, List<TrackedObject> trackedObjects, String senderName, int designatedTime) {
        this.handeledEvent = event;
        this.foundTime = time;
        this.trackedObjects = trackedObjects;
        this.senderName = senderName;
        this.designatedTime = designatedTime;
    }
    public int getTime() {
        return foundTime;
    }
    public DetectObjectsEvent getHandeledEvent(){
        return this.handeledEvent;
    }
    public int getdesignatedTime(){
        return this.designatedTime;
    }
    public List<TrackedObject> getTrackedObjects() {
        return trackedObjects;
    }
    public String getSenderName() {
        return senderName;
    }
}


   