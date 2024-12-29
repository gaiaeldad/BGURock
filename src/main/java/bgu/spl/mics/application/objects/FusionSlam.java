package bgu.spl.mics.application.objects;

import java.util.*;


/**
 * Manages the fusion of sensor data for simultaneous localization and mapping (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam exists.
 */
public class FusionSlam {
    private static class SingletonHolderFusionSlam {// מימוש כמו שהוצג בכיתה
         private static final FusionSlam INSTANCE = new FusionSlam();
    }
    public static FusionSlam getInstance() {
        return SingletonHolderFusionSlam.INSTANCE;
    }

    private ArrayList<LandMark> landmarks  = new ArrayList<>(); // Dynamic array of landmarks
    private Pose currentPose = null; // The current pose of the robot
    private int serviceCounter = 0;
    
    
    /**
     * Updates the current pose of the robot.
     *
     * @param pose The new pose.
     */
    public void updatePose(Pose pose) {
        this.currentPose = pose;
    }

    /**
     * Processes a tracked object event to update or add landmarks.
     *
     * @param trackedObjects The list of tracked objects.
     */
    public synchronized void processTrackedObjects(List<TrackedObject> trackedObjects) {
        if (currentPose != null) {
            for (TrackedObject obj : trackedObjects) {
                String id = obj.getId();
                List<CloudPoint> globalCoordinates = transformToGlobal(obj.getCoordinates(), currentPose);
    
                LandMark existingLandmark = findLandMarkById(id);
                if (existingLandmark != null) {
                    // Update existing landmark by averaging coordinates
                    updateLandmarkCoordinates(existingLandmark, globalCoordinates);
                } else {
                    // Add new landmark
                    LandMark newLandmark = new LandMark(id, obj.getDescription(), globalCoordinates);
                    landmarks.add(newLandmark);
                    StatisticalFolder.getInstance().updateNumLandmarks(1);
                }
            }
        }  
    }
    /**
     * Updates the coordinates of an existing landmark by averaging the new coordinates with the old ones.
     *
     * @param existingLandmark The existing landmark to update.
     * @param newCoordinates   The new coordinates to average with the old ones.
     */
    private void updateLandmarkCoordinates(LandMark existingLandmark, List<CloudPoint> newCoordinates) {
        List<CloudPoint> existingCoordinates = existingLandmark.getCoordinates();
        List<CloudPoint> updatedCoordinates = new ArrayList<>();

        int size = Math.min(existingCoordinates.size(), newCoordinates.size());
        for (int i = 0; i < size; i++) {
            CloudPoint oldPoint = existingCoordinates.get(i);
            CloudPoint newPoint = newCoordinates.get(i);

            double avgX = (oldPoint.getX() + newPoint.getX()) / 2;
            double avgY = (oldPoint.getY() + newPoint.getY()) / 2;

            updatedCoordinates.add(new CloudPoint(avgX, avgY));
        }

        // Add any remaining points from newCoordinates
        if (newCoordinates.size() > existingCoordinates.size()) {
            updatedCoordinates.addAll(newCoordinates.subList(size, newCoordinates.size()));
        }

        // Add any remaining points from existingCoordinates
        if (existingCoordinates.size() > newCoordinates.size()) {
            updatedCoordinates.addAll(existingCoordinates.subList(size, existingCoordinates.size()));
        }

        existingCoordinates.clear();
        existingCoordinates.addAll(updatedCoordinates);
    }


    /**
     * Finds a landmark by its ID.
     *
     * @param id The ID of the landmark.
     * @return The LandMark object if found, or null otherwise.
     */
    private LandMark findLandMarkById(String id) {
        for (LandMark landmark : landmarks) {
            if (landmark.getId().equals(id)) {
                return landmark;
            }
        }
        return null;
    }

    /**
     * Transforms local coordinates to global coordinates based on the robot's pose.
     *
     * @param localCoordinates The local coordinates to transform.
     * @param pose             The current pose of the robot.
     * @return A list of transformed global coordinates.
     */
    private List<CloudPoint> transformToGlobal(List<CloudPoint> localCoordinates, Pose pose) {
        List<CloudPoint> globalCoordinates = new ArrayList<>();

        double yawRadians = Math.toRadians(pose.getYaw());
        double cosYaw = Math.cos(yawRadians);
        double sinYaw = Math.sin(yawRadians);

        for (CloudPoint point : localCoordinates) {
            double globalX = (double) (point.getX() * cosYaw - point.getY() * sinYaw + pose.getX());
            double globalY = (double) (point.getX() * sinYaw + point.getY() * cosYaw + pose.getY());

            globalCoordinates.add(new CloudPoint(globalX, globalY));
        }

        return globalCoordinates;
    }

    /**
     * Returns the list of landmarks.
     *
     * @return A list of landmarks.
     */
    public List<LandMark> getLandmarks() {
        return Collections.unmodifiableList(new ArrayList<>(landmarks));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FusionSlam Map:\n");
        for (LandMark landmark : landmarks) {
            sb.append(landmark).append("\n");
        }
        return sb.toString();
    }
    public boolean isTerminated (){
        return (serviceCounter <= 0);
    }
    public int getserviceCounter(){
        return serviceCounter;
    }
    public void increasServiceCounter(){
        this.serviceCounter++;
    }
    public void decreaseServiceCounter() {
        this.serviceCounter--;
    }   
}