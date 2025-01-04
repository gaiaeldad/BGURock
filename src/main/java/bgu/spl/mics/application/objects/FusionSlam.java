package bgu.spl.mics.application.objects;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Manages the fusion of sensor data for simultaneous localization and mapping
 * (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update
 * a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam
 * exists.
 */
public class FusionSlam {
    private static class SingletonHolderFusionSlam {// מימוש כמו שהוצג בכיתה
        private static final FusionSlam INSTANCE = new FusionSlam();
    }

    public static FusionSlam getInstance() {
        return SingletonHolderFusionSlam.INSTANCE;
    }

    private ArrayList<LandMark> landmarks = new ArrayList<>(); // Dynamic array of landmarks
    private Map<Integer, Pose> posesByTime = new TreeMap<>(); // Map to hold poses by time
    private int serviceCounter = 1;
    private int currentTick = 0;
    // i added for main-------------
    private final AtomicInteger activeCameras = new AtomicInteger(0);
    private int activeSensors = 0;

    // Setter for active cameras
    public void setActiveCameras(int activeCameras) {
        this.activeCameras.set(activeCameras);
    }

    // Setter for total active sensors
    public void setActiveSensors(int activeSensors) {
        this.activeSensors = activeSensors;
    }

    // Getter for active cameras
    public int getActiveCameras() {
        return activeCameras.get();
    }

    // Getter for total active sensors
    public int getActiveSensors() {
        return activeSensors;
    }

    /**
     * Updates the current pose of the robot.
     *
     * @param pose The new pose.
     */
    public void addPose(Pose pose) {
        posesByTime.put(pose.getTime(), pose); // Add the pose to the map
    }

    public Pose getPoseAtTime(int time) {
        return posesByTime.get(time);
    }

    /**
     * Processes a tracked object event to update or add landmarks.
     *
     * @param trackedObjects The list of tracked objects.
     */
    public synchronized void processTrackedObjects(List<TrackedObject> trackedObjects) {
        for (TrackedObject obj : trackedObjects) {
            String id = obj.getId();
            Pose relaventPose = getPoseAtTime(obj.getTime());
            if (relaventPose == null) {// לא אמור לקרות, רק בשביל הבדיקות
                System.out.println("No pose found for time: " + obj.getTime() + ". Skipping object: " + id);
                continue; // דילוג על האובייקט אם אין פוזה מתאימה
            }
            List<CloudPoint> globalCoordinates = transformToGlobal(obj.getCoordinates(), relaventPose);
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

    /**
     * Updates the coordinates of an existing landmark by averaging the new
     * coordinates with the old ones.
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
        existingLandmark.setCoordinates(updatedCoordinates);
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
    public List<CloudPoint> transformToGlobal(List<CloudPoint> localCoordinates, Pose pose) {
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

    public void setCurrentTick(int tick) {
        this.currentTick = tick;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public boolean isTerminated() {
        return (serviceCounter <= 0);
    }

    public int getserviceCounter() {
        return serviceCounter;
    }

    public void increasServiceCounter() {
        this.serviceCounter++;
    }

    public void decreaseServiceCounter() {
        this.serviceCounter--;
    }

    public void setserviceCounter(int count) {
        this.serviceCounter = count;
    }

    public void generateOutputFile(String filePath, boolean isError, String errorDescription, String faultySensor,
            Map<String, Object> lastFrames, List<Pose> poses) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Object> outputData = new HashMap<>();

        // Add statistics
        StatisticalFolder stats = StatisticalFolder.getInstance();
        Map<String, Integer> statistics = new HashMap<>();
        statistics.put("systemRuntime", stats.getSystemRuntime());
        statistics.put("numDetectedObjects", stats.getNumDetectedObjects());
        statistics.put("numTrackedObjects", stats.getNumTrackedObjects());
        statistics.put("numLandmarks", stats.getNumLandmarks());
        outputData.put("statistics", statistics);

        // Convert landMarks to Map<String, LandMark> for JSON
        Map<String, LandMark> landMarksMap = new HashMap<>();
        for (LandMark landmark : getLandmarks()) {
            landMarksMap.put(landmark.getId(), landmark);
        }
        outputData.put("landMarks", landMarksMap);

        // Handle error-specific fields
        if (isError) {
            outputData.put("Error", errorDescription);
            outputData.put("faultySensor", faultySensor);
            outputData.put("lastFrames", lastFrames);
            outputData.put("poses", poses);
        }

        // Write JSON to file
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(outputData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Pose> getPosesUpToTick(int time) {
        List<Pose> poses = new ArrayList<>();
        for (Map.Entry<Integer, Pose> entry : posesByTime.entrySet()) {
            if (entry.getKey() <= time) {
                poses.add(entry.getValue());
            } else {
                break;
            }
        }
        return poses;
    }

}