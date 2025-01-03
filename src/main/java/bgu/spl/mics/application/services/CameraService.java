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
                    sendBroadcast(new CrashedBroadcast(camera.getErrMString(), this.getName()));
                    terminate();
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
                    sendBroadcast(new TerminatedBroadcast(getName()));
                    terminate();
                }
            } else {// camers is down
                sendBroadcast(new TerminatedBroadcast(getName()));
                terminate();
            }
        });
        // Subscribe to TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, broadcast -> {
            System.out.println(getName() + " received TerminatedBroadcast from " + broadcast.getSenderName());

            // Conditional termination: Only terminate if the sender is "TimeService"
            if ("TimeService".equals(broadcast.getSenderName())) {
                System.out.println(getName() + " terminated because TimeService has ended");
                camera.setStatus(STATUS.DOWN); // Update camera status to DOWN
                sendBroadcast(new TerminatedBroadcast(getName()));
                terminate();
            }
        });

        // Subscribe to CrashedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast broadcast) -> {
            System.out
                    .println(getName() + " received CrashedBroadcast from " + broadcast.getSenderId() + " Terminating");
            camera.setStatus(STATUS.DOWN);
            sendBroadcast(new TerminatedBroadcast(getName()));
            terminate();
        });

    }
}