package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedDetectedObject;

/**
 * An event that represents the detection of objects by the camera.
 */
// DetectObjectsEvent class
public class DetectObjectsEvent implements Event<Boolean> {
    private final StampedDetectedObject stampedDetectedObjects;
    private String senderName;
    private int sendTime; // The time when the event was sent

    public DetectObjectsEvent(StampedDetectedObject stampedDetectedObjects, String senderName, int sendTime) {
        this.stampedDetectedObjects = stampedDetectedObjects;
        this.senderName = senderName;
        this.sendTime = sendTime;
    }

    public String getSenderName() {
        return senderName;
    }

    public int getSendTime() {
        return this.sendTime;
    }

    public StampedDetectedObject getStampedDetectedObjects() {
        return stampedDetectedObjects;
    }
}