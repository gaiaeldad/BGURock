package bgu.spl.mics.application.objects;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.Event;

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

// FusionSlam class
public class FusionSlam {
    private static class SingletonHolderFusionSlam {// מימוש כמו שהוצג בכיתה
        private static final FusionSlam INSTANCE = new FusionSlam();
    }

    public static FusionSlam getInstance() {
        return SingletonHolderFusionSlam.INSTANCE;
    }

    private ArrayList<LandMark> landmarks = new ArrayList<>();
    private Map<Integer, Pose> posesByTime = new HashMap<>();
    private int tick = 0;
    private final AtomicInteger activeCameras = new AtomicInteger(0);
    private int activeSensors = 0;

    /**
     * Processes a list of tracked objects to update or add landmarks based on the
     * robot's pose.
     *
     * @pre {@code trackedObjects != null} - The provided list of tracked objects
     *      must not be null.
     * @post For each tracked object:
     *       - If a landmark with the same ID exists, its coordinates are updated.
     *       - Otherwise, a new landmark is added to the system.
     *
     * @param trackedObjects The list of tracked objects to process.
     */
    public synchronized void processTrackedObjects(List<TrackedObject> trackedObjects) {
        for (TrackedObject obj : trackedObjects) {
            String id = obj.getId();
            Pose relaventPose = getPoseAtTime(obj.getTime());
            if (relaventPose == null) {// just for test
                System.out.println("No pose found for time: " + obj.getTime() + ". Skipping object: " + id);
                continue;
            }
            List<CloudPoint> globalCoordinates = transformToGlobal(obj.getCoordinates(), relaventPose);
            LandMark existingLandmark = findLandMarkById(id);
            if (existingLandmark != null) {
                updateLandmarkCoordinates(existingLandmark, globalCoordinates);
            } else {
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
     * @pre {@code existingLandmark != null && newCoordinates != null} - Both the
     *      existing landmark and new coordinates must not be null.
     * @post The coordinates of {@code existingLandmark} are updated to reflect the
     *       average of the old and new coordinates.
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
        if (newCoordinates.size() > existingCoordinates.size()) {
            updatedCoordinates.addAll(newCoordinates.subList(size, newCoordinates.size()));
        }
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
     * @pre {@code localCoordinates != null && pose != null} - Both the list of
     *      local coordinates and the pose must not be null.
     * @post Returns a list of global coordinates transformed according to the
     *       provided pose.
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
            double globalX = point.getX() * cosYaw - point.getY() * sinYaw + pose.getX();
            double globalY = point.getX() * sinYaw + point.getY() * cosYaw + pose.getY();
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

    public void generateOutputFileWithError(String filePath, String errorDescription, String faultySensor) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Object> outputData = new LinkedHashMap<>(); // Ensure ordered output

        // Add error-specific fields

        // Add last cameras frame
        Map<String, Object> lastCamerasFrame = new LinkedHashMap<>();
        Map<String, Object> lastLiDarWorkerTrackersFrame = new LinkedHashMap<>();
        StatisticalFolder stats = StatisticalFolder.getInstance();
        Map<String, Event<?>> lastFrames = stats.getLastFrames();

        for (Map.Entry<String, Event<?>> entry : lastFrames.entrySet()) {
            String key = entry.getKey();
            Event<?> event = entry.getValue();

            if (key.startsWith("Camera")) {
                if (event instanceof DetectObjectsEvent) {
                    DetectObjectsEvent detectEvent = (DetectObjectsEvent) event;
                    StampedDetectedObject stampedDetectedObject = detectEvent.getStampedDetectedObjects();

                    // Build camera data
                    Map<String, Object> cameraData = new LinkedHashMap<>();
                    cameraData.put("time", stampedDetectedObject.getTime());

                    List<Map<String, String>> detectedObjects = new ArrayList<>();
                    for (DetectedObject obj : stampedDetectedObject.getDetectedObjects()) {
                        Map<String, String> objData = new LinkedHashMap<>();
                        objData.put("id", obj.getId());
                        objData.put("description", obj.getDescription());
                        detectedObjects.add(objData);
                    }
                    cameraData.put("detectedObjects", detectedObjects);

                    lastCamerasFrame.put(key, cameraData);
                }
            } else if (key.startsWith("LiDar")) {
                if (event instanceof TrackedObjectsEvent) {
                    TrackedObjectsEvent trackedEvent = (TrackedObjectsEvent) event;

                    List<Map<String, Object>> trackedObjects = new ArrayList<>();
                    for (TrackedObject obj : trackedEvent.getTrackedObjects()) {
                        Map<String, Object> objData = new LinkedHashMap<>();
                        objData.put("id", obj.getId());
                        objData.put("time", obj.getTime());
                        objData.put("description", obj.getDescription());
                        objData.put("coordinates", obj.getCoordinates());
                        trackedObjects.add(objData);
                    }
                    lastLiDarWorkerTrackersFrame.put(key, trackedObjects);
                }
            }
        }
        outputData.put("lastCamerasFrame", lastCamerasFrame);
        outputData.put("lastLiDarWorkerTrackersFrame", lastLiDarWorkerTrackersFrame);

        // Add poses
        outputData.put("poses", getAllPoses());
        // Add landmarks to statistics
        Map<String, Object> landmarks = new LinkedHashMap<>();
        for (LandMark landmark : getLandmarks()) {
            Map<String, Object> landmarkData = new LinkedHashMap<>();
            landmarkData.put("id", landmark.getId());
            landmarkData.put("description", landmark.getDescription());
            landmarkData.put("coordinates", landmark.getCoordinates());
            landmarks.put(landmark.getId(), landmarkData);
        }

        // Add statistics
        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("landMarks", landmarks);
        statistics.put("error", errorDescription);
        statistics.put("faultySensor", faultySensor);
        statistics.put("systemRuntime", stats.getSystemRuntime());
        statistics.put("numDetectedObjects", stats.getNumDetectedObjects());
        statistics.put("numTrackedObjects", stats.getNumTrackedObjects());
        statistics.put("numLandmarks", stats.getNumLandmarks());

        outputData.put("statistics", statistics);

        // Write JSON to file
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(outputData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateOutputFileWithoutError(String filePath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Object> outputData = new HashMap<>();

        // Add statistics
        StatisticalFolder stats = StatisticalFolder.getInstance();
        Map<String, Object> statistics = Map.of(
                "systemRuntime", stats.getSystemRuntime(),
                "numDetectedObjects", stats.getNumDetectedObjects(),
                "numTrackedObjects", stats.getNumTrackedObjects(),
                "numLandmarks", stats.getNumLandmarks());
        outputData.put("statistics", statistics);

        // Add landmarks
        Map<String, Object> landmarks = new HashMap<>();
        for (LandMark landmark : getLandmarks()) {
            landmarks.put(landmark.getId(), landmark); // Gson will handle serialization
        }
        outputData.put("landMarks", landmarks);

        // Add poses
        outputData.put("poses", getAllPoses()); // Gson will serialize the list

        // Write JSON to file
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(outputData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Pose> getAllPoses() {
        return new ArrayList<>(posesByTime.values());
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

    // Getter for active cameras
    public int getActiveCameras() {
        return activeCameras.get();
    }

    // Setter for active cameras
    public void setActiveCameras(int activeCameras) {
        this.activeCameras.set(activeCameras);
    }

    // Getter for total active sensors
    public int getActiveSensors() {
        return activeSensors;
    }

    // Setter for total active sensors
    public void setActiveSensors(int activeSensors) {
        this.activeSensors = activeSensors;
    }

    public boolean isTerminated() {
        return (this.activeSensors <= 0);
    }

    public int getserviceCounter() {
        return activeSensors;
    }

    public void decreaseServiceCounter() {
        this.activeSensors--;
    }

    /**
     * Adds a new pose to the system, associating it with the robot's current state
     * at a specific time.
     *
     * @pre {@code pose != null} - The provided pose must not be null.
     * @post The pose is added to the internal map, and can be retrieved using
     *       {@link #getPoseAtTime(int)}.
     *
     * @param pose The new pose to add.
     */
    public void addPose(Pose pose) {
        posesByTime.put(pose.getTime(), pose);

    }

    public Pose getPoseAtTime(int time) {
        return posesByTime.get(time);
    }

    public void setTick(int time) {
        this.tick = time;
    }

    public int getTick() {
        return tick;
    }

}