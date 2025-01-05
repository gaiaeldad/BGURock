package bgu.spl.mics.application.services;

import bgu.spl.mics.*;
import java.util.PriorityQueue;
import java.nio.file.Paths;
import java.util.Comparator;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.messages.*;

/**
 * FusionSlamService is a MicroService that processes TrackedObjectsEvents and
 * PoseEvents to update the global map in the FusionSLAM object.
 */
// FusionSlamService class

public class FusionSlamService extends MicroService {
    private final FusionSlam fusionSlam;
    private PriorityQueue<TrackedObjectsEvent> TrackedObjectsQueue = new PriorityQueue<>(
            Comparator.comparingInt(e -> e.getTrackedObjects().get(0).getTime()));
    private String outputFilePath;

    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global
     *                   map.
     */
    public FusionSlamService(FusionSlam fusionSlam, String configDirectory) {
        super("FusionSlamService");
        this.fusionSlam = FusionSlam.getInstance();
        this.outputFilePath = Paths.get(configDirectory, "output_file.json").toString();
    }

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle events and broadcasts.
     */
    @Override
    protected void initialize() {
        // Register for TrackedObjectsEvent
        subscribeEvent(TrackedObjectsEvent.class, event -> {
            System.out.println(getName() + ": recived TrackedObjectsEvent");
            if (fusionSlam.getPoseAtTime(event.getTrackedObjects().get(0).getTime()) == null) {
                System.out.println("this event had no pose");
                TrackedObjectsQueue.add(event);
            } else {
                fusionSlam.processTrackedObjects(event.getTrackedObjects());
                System.out.println(getName() + "processed TrackedObjectsEvent from time" + event.getTime());
                complete(event, true);
            }

        });

        // Register for PoseEvent
        subscribeEvent(PoseEvent.class, event -> {
            System.out.println(getName() + ": recived PoseEvent");
            fusionSlam.addPose(event.getPose());
            System.out.println("PoseEvent from " + event.getPose().getTime() + " has been processed in: " + getName());
            complete(event, true);
            // Process all TrackedObjectsEvents in the queue that have a corresponding Pose
            while (!TrackedObjectsQueue.isEmpty()) {
                int trackedObjectsTime = TrackedObjectsQueue.peek().getTime();
                // Check if there is a Pose available for the given time and process the event
                if (fusionSlam.getPoseAtTime(trackedObjectsTime) != null) {
                    TrackedObjectsEvent e = TrackedObjectsQueue.poll();
                    fusionSlam.processTrackedObjects(e.getTrackedObjects());
                    System.out
                            .println("the poseevent has been processed in: " + getName() + " at time: " + e.getTime());
                    complete(e, true);
                } else {
                    // Stop processing if the required Pose is not available
                    break;
                }
            }
        });
        ;

        // Register for TickBroadcast
        subscribeBroadcast(TickBroadcast.class, broadcast -> {
            System.out.println(getName() + ": recived a tickBrodcast, tick: " + broadcast.getTime());
            int currentTick = broadcast.getTime();
            fusionSlam.setTick(currentTick);

        });

        // Register for TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, broadcast -> {
            if (broadcast.getSenderName() != "TimeService ") {
                fusionSlam.decreaseServiceCounter();
                System.out.println(getName() + ": service counter is " + fusionSlam.getserviceCounter()
                        + "termineted sensor: " + broadcast.getSenderName());
                if (fusionSlam.getserviceCounter() == 0) {
                    System.out.println(getName() + ": terminate program, service counter is 0");
                    terminate();
                    System.out.println(getName() + ": has terminated");
                    System.out.println(getName() + ": is printing an output file");
                    fusionSlam.generateOutputFileWithoutError(outputFilePath);
                }
            }
        });

        // Register for CrashedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, broadcast -> {
            System.out.println(getName() + ": recived CrashedBroadcast from " + broadcast.getSenderName());
            terminate();
            String errorDescription = broadcast.getErrorMessage(); // Populate if isError = true
            String faultySensor = broadcast.getSenderName(); // Populate if isError = true
            System.out.println(getName() + ": is printing an error output file");
            fusionSlam.generateOutputFileWithError(outputFilePath, errorDescription, faultySensor);
        });
    }
}