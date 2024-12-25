package bgu.spl.mics.application.objects;

import java.util.List;

/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */
public class Camera {
    
    private int id;
    private int frequency;
    private STATUS status;  // The status of the camera (UP, DOWN, ERROR)
    private List<StampedDetectedObject> detectedObjectsList;// צריך ליצור מחלקה כזאת

    // Constructor for Camera.
    public Camera(int id, int frequency, STATUS status, List<StampedDetectedObject> detectedObjectsList) {
        this.id = id;
        this.frequency = frequency;
        this.status = status;
        this.detectedObjectsList = detectedObjectsList;
    }

    public Camera(int id, int frequency, String statusString, List<StampedDetectedObject> detectedObjectsList) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.fromString(statusString);  // Convert the string to the corresponding Status
        this.detectedObjectsList = detectedObjectsList;
    }


    // Getters and setters for the fields.
    public int getId() {
        return id;
    }

    public int getFrequency() {
        return frequency;
    }

    public STATUS getStatus() {
        return status;
    }
  
    public List<StampedDetectedObject> getDetectedObjectsList() {
        return detectedObjectsList;
    }

    // Method to simulate the camera detecting an object and adding it to the list.
    public void detectObject(StampedDetectedObject detectedObject) {
        detectedObjectsList.add(detectedObject);
    }

    public StampedDetectedObject getDetectedObjectsAtTime(int time) {
        for (StampedDetectedObject stampedObject : detectedObjectsList) {
            if (stampedObject.getTime() == time) {
                return stampedObject;
            }
        }
        return null; // Return an empty list if no objects were detected at the given time
    }
    
    

    // Method to simulate the camera sending events.
    // אנחנו צריכים או שזה  callback?
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
    public void setDetectedObjectsList(List<StampedDetectedObject> detectedObjectsList) {
        this.detectedObjectsList = detectedObjectsList;
    }
*/
}