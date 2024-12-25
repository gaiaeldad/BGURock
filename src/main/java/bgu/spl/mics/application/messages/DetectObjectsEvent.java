package bgu.spl.mics.application.messages;
import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedDetectedObject;

public class DetectObjectsEvent implements Event<Boolean> {
    private final StampedDetectedObject stampedDetectedObjects;
    private String senderName;
    

    public DetectObjectsEvent(StampedDetectedObject stampedDetectedObjects,String senderName) {
        this.stampedDetectedObjects = stampedDetectedObjects;
        this.senderName = senderName;
    }

    public StampedDetectedObject getStampedDetectedObjects() {
        return stampedDetectedObjects;
}
    public String getSenderName() {
    return senderName;
}
} 