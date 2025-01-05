package bgu.spl.mics.application.objects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import bgu.spl.mics.Event;

public class StatisticalFolder {
    // Singleton class to store the statistics of the system

    private AtomicInteger systemRuntime; // The total runtime of the system (in ticks)
    private AtomicInteger numDetectedObjects; // The cumulative count of objects detected by all cameras
    private AtomicInteger numTrackedObjects; // The cumulative count of objects tracked by all LiDAR workers
    private AtomicInteger numLandmarks; // The total number of unique landmarks identified
    private Map<String, Event<?>> lastFrames; // The last frames of all cameras

    // Constructor
    public StatisticalFolder() {
        this.systemRuntime = new AtomicInteger(0);
        this.numDetectedObjects = new AtomicInteger(0);
        this.numTrackedObjects = new AtomicInteger(0);
        this.numLandmarks = new AtomicInteger(0);
        this.lastFrames = new ConcurrentHashMap<>();
    }

    // Singleton Holder implementation as shown in class
    private static class SingletonHolderStatisticalFolder {
        private static final StatisticalFolder INSTANCE = new StatisticalFolder();
    }

    public static StatisticalFolder getInstance() {
        return SingletonHolderStatisticalFolder.INSTANCE;
    }

    // Methods to update the statistics
    public void IncrementSystemRuntime() {
        this.systemRuntime.incrementAndGet(); // Increment system runtime by the time tick
    }

    public void updateNumDetectedObjects(int detectedObjectsCount) {
        this.numDetectedObjects.addAndGet(detectedObjectsCount); // Increment detected objects count
    }

    public void updateNumTrackedObjects(int trackedObjectsCount) {
        this.numTrackedObjects.addAndGet(trackedObjectsCount); // Increment tracked objects count
    }

    public void updateNumLandmarks(int newLandmarksCount) {
        this.numLandmarks.addAndGet(newLandmarksCount); // Increment landmarks count
    }

    public void updateLastFrame(String name, Event<?> event) {
        lastFrames.put(name, event);
    }

    public Map<String, Event<?>> getLastFrames() {
        return lastFrames;
    }

    // Getters
    public int getSystemRuntime() {
        return systemRuntime.get();
    }

    public int getNumDetectedObjects() {
        return numDetectedObjects.get();
    }

    public int getNumTrackedObjects() {
        return numTrackedObjects.get();
    }

    public int getNumLandmarks() {
        return numLandmarks.get();
    }

    // Add a reset method for testing
    public void reset() {
        systemRuntime.set(0);
        numDetectedObjects.set(0);
        numTrackedObjects.set(0);
        numLandmarks.set(0);
    }
}
