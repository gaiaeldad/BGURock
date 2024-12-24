package bgu.spl.mics.application.objects;

import java.util.List;

/**
 * Represents a group of cloud points corresponding to a specific timestamp.
 * Used by the LiDAR system to store and process point cloud data for tracked objects.
 */
public class StampedCloudPoints {

    private String id;  // The ID of the object
    private int time;  // The time the object was tracked
    private List<List<Double>> cloudPoints;  // List of lists of cloud points (coordinates)

    public StampedCloudPoints(String id, int time, List<List<Double>> cloudPoints) {
        this.id = id;
        this.time = time;
        this.cloudPoints = cloudPoints;
    }

    // Getters and Setters for id, time, and cloudPoints

    public String getId() {
        return id;
    }

    public int getTime() {
        return time;
    }

    public List<List<Double>> getCloudPoints() {
        return cloudPoints;
    }


    /**
     * Add a new cloud point (coordinate pair) to the list of cloud points.
     * 
     * @param cloudPoint The cloud point (coordinates) to be added as a list of doubles.
     */
    public void addCloudPoint(List<Double> cloudPoint) {
        cloudPoints.add(cloudPoint);
    }

    @Override
    public String toString() {
        return "StampedCloudPoints{id='" + id + "', time=" + time + ", cloudPoints=" + cloudPoints + "}";
    }
/* 
    public void setId(String id) {
        this.id = id;
    }
    public void setTime(int time) {
        this.time = time;
    }
    public void setCloudPoints(List<List<Double>> cloudPoints) {
        this.cloudPoints = cloudPoints;
    }
*/
}