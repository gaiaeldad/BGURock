package bgu.spl.mics.application.services;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {
    private final Camera camera;

    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera) {
        super("CameraService");
        this.camera = camera;
    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts and sets up callbacks for sending
     * DetectObjectsEvents.
     */
    @Override
    protected void initialize() {
        // Subscribe to TickBroadcast to act on each time tick
        subscribeBroadcast(TickBroadcast.class, broadcast -> {
            int currentTime = broadcast.getTime();

            // Check if it's time to send a DetectObjectsEvent based on the camera's frequency
            if (currentTime % camera.getFrequency() == 0) {
                // Retrieve detected objects
                var detectedObjects = camera.detectObjects(currentTime);

                // If there are detected objects, create and send a DetectObjectsEvent
                if (!detectedObjects.isEmpty()) {
                    DetectObjectsEvent event = new DetectObjectsEvent(detectedObjects, currentTime);
                    sendEvent(event);

                    // Update the statistical folder
                    camera.getStatisticalFolder().incrementDetectedObjects(detectedObjects.size());
                }
            }
        });

        // Subscribe to Termination Broadcast to cleanly shut down the service
        subscribeBroadcast(Broadcast.class, broadcast -> {
            terminate();
        });
    }
}
