package bgu.spl.mics.application.objects;

import java.util.concurrent.atomic.AtomicInteger;

public class StatisticalFolder {
  
    // Fields using AtomicInteger for thread-safety
    private AtomicInteger systemRuntime;          // The total runtime of the system (in ticks)
    private AtomicInteger numDetectedObjects;     // The cumulative count of objects detected by all cameras
    private AtomicInteger numTrackedObjects;      // The cumulative count of objects tracked by all LiDAR workers
    private AtomicInteger numLandmarks;           // The total number of unique landmarks identified

    // Constructor
    public StatisticalFolder() {
        this.systemRuntime = new AtomicInteger(0);
        this.numDetectedObjects = new AtomicInteger(0);
        this.numTrackedObjects = new AtomicInteger(0);
        this.numLandmarks = new AtomicInteger(0);
    }
    // Singleton Holder for thread-safe מימוש כמו בכיתה
    private static class SingletonHolderStatisticalFolder {
        private static final StatisticalFolder INSTANCE = new StatisticalFolder();
    }

    public static StatisticalFolder getInstance() {
        return SingletonHolderStatisticalFolder.INSTANCE;
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

    // Methods to update the statistics
    public void updateSystemRuntime(int timeTick) {
        this.systemRuntime.addAndGet(timeTick); // Increment system runtime by the time tick
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
}