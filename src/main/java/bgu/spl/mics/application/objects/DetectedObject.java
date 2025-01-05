package bgu.spl.mics.application.objects;

/**
 * Represents an object detected by the camera.
 */
// detected object class
public class DetectedObject {

    private String id;
    private String description;

    public DetectedObject(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}