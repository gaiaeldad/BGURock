package bgu.spl.mics.application.objects;

import java.util.List;

/**
 * Represents a landmark in the environment map.
 * Landmarks are identified and updated by the FusionSlam service.
 */
public class LandMark {
    // Fields
    private String id;  // The internal ID of the landmark
    private String description;  // Description of the landmark
    private List<CloudPoint> coordinates;  // List of coordinates representing the landmark

    // Constructor to initialize the LandMark object
    public LandMark(String id, String description, List<CloudPoint> coordinates) {
        this.id = id;
        this.description = description;
        this.coordinates = coordinates;
    }

    // Getters and Setters

    // Get the ID of the landmark
    public String getId() {
        return id;
    }

    // Get the description of the landmark
    public String getDescription() {
        return description;
    }

    // Get the coordinates of the landmark
    public List<CloudPoint> getCoordinates() {
        return coordinates;
    }
    
    // Add a CloudPoint to the list of coordinates
    public void addCoordinate(CloudPoint coordinate) {
        this.coordinates.add(coordinate);
        StatisticalFolder.getInstance().updateNumLandmarks(1);  // מעדכן את מספר ה-landmarks במערכת.
    }

    // Optional: Override toString() to provide a string representation of the landmark
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LandMark{id='").append(id)
          .append("', description='").append(description)
          .append("', coordinates=");
        
        // Print all coordinates
        for (CloudPoint point : coordinates) {
            sb.append(point).append(", ");
        }
        sb.append("}");
        return sb.toString();
    }
/* 
    public void setId(String id) {
        this.id = id;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setCoordinates(List<CloudPoint> coordinates) {
        this.coordinates = coordinates;
    }
*/

}