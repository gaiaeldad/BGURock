package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

//
/**
 * Represents an object tracked by the LiDAR.
 * This object includes information about the tracked object's ID, description,
 * time of tracking, and coordinates in the environment.
 */
// TrackedObject class
public class TrackedObject {
    // fields

    private String id; // The ID of the tracked object
    private int time; // The time the object was tracked
    private String description; // A description of the object
    private List<CloudPoint> coordinates; // The coordinates of the object, represented by a list of CloudPoint objects

    /**
     * Constructor to initialize a TrackedObject with all fields.
     * 
     * @param id          The ID of the tracked object.
     * @param time        The time the object was tracked.
     * @param description A description of the tracked object.
     * @param coordinates A list of CloudPoint objects representing the object's
     *                    coordinates.
     */
    public TrackedObject(String id, int time, String description, List<CloudPoint> coordinates) {
        this.id = id;
        this.time = time;
        this.description = description;
        this.coordinates = coordinates != null ? coordinates : new ArrayList<>();
    }

    // Getters and Setters

    public int getTime() {
        return time;
    }

    public String getId() {
        return id;
    }

    public List<CloudPoint> getCoordinates() {
        return coordinates;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Add a single coordinate point to the coordinates list.
     * 
     * @param cloudPoint The CloudPoint object to be added.
     */
    public void addCoordinate(CloudPoint cloudPoint) {
        coordinates.add(cloudPoint);
    }

    @Override
    public String toString() {
        return "TrackedObject{id='" + id + "', time=" + time + ", description='" + description + "', coordinates="
                + coordinates + "}";
    }
}