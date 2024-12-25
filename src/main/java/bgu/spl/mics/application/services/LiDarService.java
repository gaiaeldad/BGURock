package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.TrackedObject;
import bgu.spl.mics.application.objects.StatisticalFolder;
import java.util.List;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 * 
 * This service interacts with the LiDarWorkerTracker object to retrieve and process
 * cloud point data and updates the system's StatisticalFolder upon sending its
 * observations.
 */
public class LiDarService extends MicroService {

    private final LiDarWorkerTracker lidarWorkerTracker;

    /**
     * Constructor for LiDarService.
     *
     * @param name The name of the service.
     * @param lidarWorkerTracker A LiDAR Tracker worker object that this service will use to process data.
     */
    public LiDarService(String name, LiDarWorkerTracker lidarWorkerTracker) {
        super(name);
        this.lidarWorkerTracker = lidarWorkerTracker;
    }

    /**
     * Initializes the LiDarService.
     * Registers the service to handle DetectObjectsEvents and TickBroadcasts,
     * and sets up the necessary callbacks for processing data.
     */
    @Override
    protected void initialize() {
        // Subscribe to TickBroadcast
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast broadcast) -> {
            int currentTime = broadcast.getTime();

            // Process data if the LiDAR is active and it's time to send updates
            if (lidarWorkerTracker.getStatus() == STATUS.UP) {
                List<TrackedObject> trackedObjects = lidarWorkerTracker.getTrackedObjectsAtTime(currentTime + lidarWorkerTracker.getFrequency());

                if (trackedObjects != null && !trackedObjects.isEmpty()) {
                    // Send a single TrackedObjectsEvent with the list of tracked objects
                    TrackedObjectsEvent event = new TrackedObjectsEvent(trackedObjects, currentTime);
                    sendEvent(event);

                    // Update statistical data
                    StatisticalFolder.getInstance().updateNumTrackedObjects(1); 
                }
            }
        });

        // Subscribe to DetectObjectsEvent
        subscribeEvent(DetectObjectsEvent.class, (DetectObjectsEvent event) -> {
            lidarWorkerTracker.processDetectObjectsEvent(event);
        });

        // Subscribe to TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast broadcast) -> {
            terminate();
        });
    }
}
