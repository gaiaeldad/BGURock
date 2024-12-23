import java.util.concurrent.atomic.AtomicInteger;

public class StatisticalFolder {
    private static StatisticalFolder instance = null;

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
    public static StatisticalFolder getInstance() {
        if (instance == null) {
            instance = new StatisticalFolder(); // Create the instance if it doesn't exist
        }
        return instance;
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

    // Setters (Optional)
    public void setSystemRuntime(int systemRuntime) {
        this.systemRuntime.set(systemRuntime);
    }

    public void setNumDetectedObjects(int numDetectedObjects) {
        this.numDetectedObjects.set(numDetectedObjects);
    }

    public void setNumTrackedObjects(int numTrackedObjects) {
        this.numTrackedObjects.set(numTrackedObjects);
    }

    public void setNumLandmarks(int numLandmarks) {
        this.numLandmarks.set(numLandmarks);
    }

    // Methods to update the statistics
    public void updateSystemRuntime(int timeTick) {
        this.systemRuntime.addAndGet(timeTick);  // Increment system runtime by the time tick
    }

    public void updateNumDetectedObjects(int detectedObjectsCount) {
        this.numDetectedObjects.addAndGet(detectedObjectsCount);  // Increment detected objects count
    }

    public void updateNumTrackedObjects(int trackedObjectsCount) {
        this.numTrackedObjects.addAndGet(trackedObjectsCount);  // Increment tracked objects count
    }

    public void updateNumLandmarks(int newLandmarksCount) {
        this.numLandmarks.addAndGet(newLandmarksCount);  // Increment landmarks count
    }
}