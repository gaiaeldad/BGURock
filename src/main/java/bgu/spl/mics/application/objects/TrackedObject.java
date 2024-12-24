package bgu.spl.mics.application.objects;

/**
 * Represents an object tracked by the LiDAR.
 * This object includes information about the tracked object's ID, description, 
 * time of tracking, and coordinates in the environment.
 */
public class TrackedObject {

    private String id;  // The ID of the tracked object
    private int time;  // The time the object was tracked
    private String description;  // A description of the object
    private CloudPoint[] coordinates;  // The coordinates of the object, represented by an array of CloudPoint objects

    /**
     * Constructor to initialize a TrackedObject with all fields.
     * 
     * @param id The ID of the tracked object.
     * @param time The time the object was tracked.
     * @param description A description of the tracked object.
     * @param coordinates An array of CloudPoint objects representing the object's coordinates.
     */
    public TrackedObject(String id, int time, String description, CloudPoint[] coordinates) {
        this.id = id;
        this.time = time;
        this.description = description;
        this.coordinates = coordinates;
    }

    // Getters and Setters for each field

    public String getId() {
        return id;
    }

    public int getTime() {
        return time;
    }

    public String getDescription() {
        return description;
    }

    public CloudPoint[] getCoordinates() {
        return coordinates;
    }

    /**
     * Add a single coordinate point to the coordinates array.
     * 
     * @param cloudPoint The CloudPoint object to be added.
     */
    public void addCoordinate(CloudPoint cloudPoint) {
        CloudPoint[] newCoordinates = new CloudPoint[coordinates.length + 1];
        System.arraycopy(coordinates, 0, newCoordinates, 0, coordinates.length);
        newCoordinates[coordinates.length] = cloudPoint;       
        coordinates = newCoordinates;
    }
/* 
    public void setId(String id) {
        this.id = id;
    }
    public void setTime(int time) {
        this.time = time;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setCoordinates(CloudPoint[] coordinates) {
        this.coordinates = coordinates;
    }
*/
}