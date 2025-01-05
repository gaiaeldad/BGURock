package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of cloud points corresponding to a specific timestamp.
 * Used by the LiDAR system to store and process point cloud data for tracked
 * objects.
 */
// StampedCloudPoints class

public class StampedCloudPoints {
    // fields
    private int time;
    private String id;
    private List<List<Double>> cloudPoints;

    public StampedCloudPoints(int time, String id, List<List<Double>> cloudPoints) {
        this.time = time;
        this.id = id;
        this.cloudPoints = cloudPoints;
    }

    // getters
    public int getTime() {
        return time;
    }

    public String getId() {
        return id;
    }

    public List<List<Double>> getCloudPoints() {
        return cloudPoints;
    }

    /**
     * Adds a single cloud point as a List<Double> to the cloudPoints list.
     * 
     * @param cloudPoint A list containing the x and y coordinates of the point.
     */
    public void addCloudPoint(List<Double> cloudPoint) {
        cloudPoints.add(cloudPoint);
    }

    /**
     * Converts the List<List<Double>> to a List<CloudPoint>.
     * 
     * @return A list of CloudPoint objects.
     */
    public List<CloudPoint> listToCloudPoints() {
        List<CloudPoint> cloudPointList = new ArrayList<>();
        for (List<Double> point : cloudPoints) {
            if (point.size() >= 2) { // Ensure at least X and Y coordinates are present
                double x = point.get(0);
                double y = point.get(1);
                cloudPointList.add(new CloudPoint(x, y));
            }
        }
        return cloudPointList;
    }

    @Override
    public String toString() {
        return "StampedCloudPoints{id='" + id + "', time=" + time + ", cloudPoints=" + cloudPoints + "}";
    }
}