package bgu.spl.mics.application.services;

import java.util.ArrayDeque;
//import java.util.List;
import java.util.Queue;

//import bgu.spl.mics.Broadcast;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.StampedDetectedObject;
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
    private Queue<DetectObjectsEvent> eventQueue;

    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera) {
        super("CameraService" + camera.getId());
        this.camera = camera;
        this.eventQueue = new ArrayDeque<>();

    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts and sets up callbacks for
     * sending
     * DetectObjectsEvents.
     */
    @Override
    protected void initialize() {
        // Subscribe to TickBroadcast
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast broadcast) -> {
            int currentTime = broadcast.getTime();
            System.out.println(getName() + ": got a tick, " + currentTime + " and status is: " + camera.getStatus());

            // Check if the camera is active and it's time to send an event

            if (camera.getStatus() == STATUS.UP) {
                StampedDetectedObject detectedObject = camera.getDetectedObjectsAtTime(currentTime);
                if (camera.getStatus() == STATUS.ERROR) {
                    System.out.println(getName() + ": got an error");
                    terminate();
                    sendBroadcast(new CrashedBroadcast(camera.getErrMString(), this.getName()));
                } else {
                    if (detectedObject != null) {
                        int sendTime = currentTime + camera.getFrequency();
                        DetectObjectsEvent event = new DetectObjectsEvent(detectedObject, getName(), sendTime);
                        eventQueue.add(event);
                    }
                    // Process events that are ready to be sent
                    while (!eventQueue.isEmpty()) {
                        DetectObjectsEvent event = eventQueue.peek();
                        if (event.getSendTime() > currentTime) {
                            break; // If the first event is not ready, stop processing
                        }
                        DetectObjectsEvent readyEvent = eventQueue.poll(); // Remove the first event (FIFO)
                        sendEvent(readyEvent);
                        System.out.println(getName() + ": sent an event");
                        StatisticalFolder.getInstance().updateNumDetectedObjects(
                                readyEvent.getStampedDetectedObjects().getDetectedObjects().size());
                    }
                }
                if (camera.getStatus() == STATUS.DOWN) {
                    System.out.println(getName() + ": is down so terminating");
                    terminate();
                    sendBroadcast(new TerminatedBroadcast(getName()));
                }
            } else {// camers is down
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));
            }
        });
        // --------------------------------------זה לא נכון צריך לתקן ולהבין מה לעשות עם
        // ההרשמות האלו ------------------------------------------------------------
        // Subscribe to TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast broadcast) -> {
            if ("TimeService".equals(broadcast.getSenderName())) {
                camera.setStatus(STATUS.DOWN);
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));
            }
        });
        // if its from the other services we think do nothing

        // Subscribe to TerminatedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast broadcast) -> {
            camera.setStatus(STATUS.DOWN);
            terminate();
            sendBroadcast(new TerminatedBroadcast(getName()));
        });

    }
}