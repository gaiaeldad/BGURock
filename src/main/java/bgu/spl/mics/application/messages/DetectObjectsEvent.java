package bgu.spl.mics.application.messages;
import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedDetectedObject;

public class DetectObjectsEvent implements Event<Boolean> {
    private final StampedDetectedObject stampedDetectedObjects;
    private String senderName;
    private int sendTime;
    

    public DetectObjectsEvent(StampedDetectedObject stampedDetectedObjects,String senderName, int sendTime) {
        this.stampedDetectedObjects = stampedDetectedObjects;
        this.senderName = senderName;
        this.sendTime = sendTime;
    }

    public StampedDetectedObject getStampedDetectedObjects() {
        return stampedDetectedObjects;
    }
    public String getSenderName() {
        return senderName;
    }
    public int getSendTime(){
        return this.sendTime;
    }
} 