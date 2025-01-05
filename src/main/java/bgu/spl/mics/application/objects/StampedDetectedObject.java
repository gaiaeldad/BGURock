package bgu.spl.mics.application.objects;

import java.util.List;

/**
 * Represents a list of detected objects with a timestamp.
 */
public class StampedDetectedObject {

    private int time; // The time when objects were detected
    private List<DetectedObject> detectedObjects; // List of objects detected at the given time

    // Constructor for StampedDetectedObject
    public StampedDetectedObject(int time, List<DetectedObject> detectedObjects) {
        this.time = time;
        this.detectedObjects = detectedObjects;
    }

    // Getters and setters

    public List<DetectedObject> getDetectedObjects() {
        return detectedObjects;
    }

    public void addDetectedObject(DetectedObject detectedObject) {
        this.detectedObjects.add(detectedObject);
    }

    public int getTime() {
        return time;
    }
}