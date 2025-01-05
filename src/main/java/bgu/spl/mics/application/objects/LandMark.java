package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a landmark in the environment map.
 * Landmarks are identified and updated by the FusionSlam service.
 */
// landmark class
public class LandMark {
    private String id; // The ID of the landmark
    private String description; // Description of the landmark
    private List<CloudPoint> coordinates; // List of coordinates representing the landmark

    // Constructor to initialize the LandMark object
    public LandMark(String id, String description, List<CloudPoint> coordinates) {
        this.id = id;
        this.description = description;
        this.coordinates = coordinates;
    }

    // Getters and Setters
    // Get the description of the landmark
    public String getDescription() {
        return description;
    }

    // Get the ID of the landmark
    public String getId() {
        return id;
    }

    // Set the coordinates of the landmark
    public void setCoordinates(List<CloudPoint> coordinates) {
        this.coordinates = new ArrayList<>(coordinates);
    }

    // Get the coordinates of the landmark
    public List<CloudPoint> getCoordinates() {
        return coordinates;
    }

    // Add a CloudPoint to the list of coordinates
    public void addCoordinate(CloudPoint coordinate) {
        this.coordinates.add(coordinate);
        StatisticalFolder.getInstance().updateNumLandmarks(1); // Update the number of landmarks in stsistics
    }

    @Override
    public String toString() {
        return String.format("{\"id\":\"%s\",\"description\":\"%s\",\"coordinates\":%s}", id, description, coordinates);
    }
}