package bgu.spl.mics.application.services;

import java.util.List;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StatisticalFolder;

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
        // Subscribe to TickBroadcast
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast broadcast) -> {
            int currentTime = broadcast.getTime();

            // Check if the camera is active and it's time to send an event
            if (camera.getStatus() == STATUS.UP && currentTime % camera.getFrequency() == 0) {
                List<DetectedObject> detectedObjects = camera.detectObject(currentTime);

                if (detectedObjects != null && !detectedObjects.isEmpty()) {
                    DetectObjectsEvent event = new DetectObjectsEvent(detectedObjects, currentTime);
                    sendEvent(event);

                    // Update the statistical folder
                    StatisticalFolder.getInstance().updateNumDetectedObjects(detectedObjects.size());
                }
            }
        });

        // Subscribe to TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast broadcast) -> {
            terminate();
        });
    }


}
