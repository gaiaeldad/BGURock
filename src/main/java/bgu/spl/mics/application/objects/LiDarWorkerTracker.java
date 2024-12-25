package bgu.spl.mics.application.objects;

import java.util.List;

/**
 * Represents a LiDar worker on the robot.
 * Responsible for tracking objects in the environment at regular intervals.
 */
public class LiDarWorkerTracker {

    private int id;  // The ID of the LiDar worker
    private int frequency;  // The frequency at which the LiDar sends new events
    private STATUS status;  // The status of the LiDar (Up, Down, Error)
    private List<TrackedObject> lastTrackedObjects;  // The list of last tracked objects

    // Constructor to initialize the LiDarWorkerTracker object.
    public LiDarWorkerTracker(int id, int frequency, STATUS status, List<TrackedObject> lastTrackedObjects) {
        this.id = id;
        this.frequency = frequency;
        this.status = status;
        this.lastTrackedObjects = lastTrackedObjects;//do we need this or emtpy 
    }
    public LiDarWorkerTracker(int id, int frequency, String statusString, List<TrackedObject> lastTrackedObjects) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.fromString(statusString); // Convert the string to the corresponding Status
        this.lastTrackedObjects = lastTrackedObjects;
    }

    // Getter and Setter for id
    public int getId() {
        return id;
    }

    // Getter and Setter for frequency
    public int getFrequency() {
        return frequency;
    }

    // Getter and Setter for status
    public STATUS  getStatus() {
        return status;
    }

    // Getter and Setter for lastTrackedObjects
    public List<TrackedObject> getLastTrackedObjects() {
        return lastTrackedObjects;
    }

    // Method to add a new TrackedObject to lastTrackedObjects list
    public void addTrackedObject(TrackedObject trackedObject) {
        this.lastTrackedObjects.add(trackedObject);
    }
/* 
    public void setId(int id) {
        this.id = id;
    }
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public void setStatus(String statusString) {
        this.status = STATUS.fromString(statusString);  // Use the helper method to set the status
    }
    public void setLastTrackedObjects(List<TrackedObject> lastTrackedObjects) {
        this.lastTrackedObjects = lastTrackedObjects;
    }
*/
}