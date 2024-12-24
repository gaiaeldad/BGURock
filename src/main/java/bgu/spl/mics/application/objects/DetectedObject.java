package bgu.spl.mics.application.objects;

/**
 * Represents an object detected by the camera.
 */
public class DetectedObject {

    private String id;  // The ID of the detected object
    private String description;  // Description of the detected object

    // Constructor for DetectedObject
    public DetectedObject(String id, String description) {
        this.id = id;
        this.description = description;
    }

    // Getter for id
    public String getId() {
        return id;
    }

    // Getter for description
    public String getDescription() {
        return description;
    }
/* 
    public void setId(String id) {
        this.id = id;
    }
    public void setDescription(String description) {
        this.description = description;
    }
*/
}