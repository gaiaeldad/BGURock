package bgu.spl.mics.application.services;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import bgu.spl.mics.*;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.messages.*;

/**
 * FusionSlamService integrates data from multiple sensors to build and update
 * the robot's global map.
 */
public class FusionSlamService extends MicroService {
    private final FusionSlam fusionSlam;
    private PriorityQueue<TrackedObjectsEvent> TrackedObjectsQueue = new PriorityQueue<>(
            Comparator.comparingInt(e -> e.getTrackedObjects().get(0).getTime()));

    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global
     *                   map.
     */
    public FusionSlamService(FusionSlam fusionSlam) {
        super("FusionSlamService");
        this.fusionSlam = FusionSlam.getInstance();
    }

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle events and broadcasts.
     */
    @Override
    protected void initialize() {
        // Register for TrackedObjectsEvent
        subscribeEvent(TrackedObjectsEvent.class, event -> {
            if (fusionSlam.getPoseAtTime(event.getTrackedObjects().get(0).getTime()) == null) {
                System.out.println("this event had no pose");
                TrackedObjectsQueue.add(event);
            } else {
                fusionSlam.processTrackedObjects(event.getTrackedObjects());
                System.out.println("the  tracked object event has been processed in: " + getName());
                complete(event, true);
            }

        });

        // Register for PoseEvent
        subscribeEvent(PoseEvent.class, event -> {
            fusionSlam.addPose(event.getPose());
            complete(event, true);
            // Process all TrackedObjectsEvents in the queue that have a corresponding Pose
            while (!TrackedObjectsQueue.isEmpty()) {
                int trackedObjectsTime = TrackedObjectsQueue.peek().getTime();
                // Check if there is a Pose available for the given time and process the event
                if (fusionSlam.getPoseAtTime(trackedObjectsTime) != null) {
                    TrackedObjectsEvent e = TrackedObjectsQueue.poll();
                    fusionSlam.processTrackedObjects(e.getTrackedObjects());
                    System.out.println("the poseevent has been processed in: " + getName());
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
            int currentTick = broadcast.getTime();
            fusionSlam.setCurrentTick(currentTick);

        });

        // Register for TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, broadcast -> {//// לעדכן שלא הגיע מטיים
            if (broadcast.getSenderName() != "TimeService") {
                fusionSlam.decreaseServiceCounter();
            }
            if (fusionSlam.getserviceCounter() == 0) {
                // Generate output file
                terminate();
                System.out.println(getName() + ": is terminated");
                Map<String, Object> lastFrames = new HashMap<>(); // Populate if isError = true
                int currentTick = fusionSlam.getCurrentTick();
                List<Pose> poses = fusionSlam.getPosesUpToTick(currentTick);
                fusionSlam.generateOutputFile("output_file.json", false, null, null, lastFrames, poses);// איפה הקובץ?
            }

        });
        // ----------------לתקן last franme רשימה ריקה ---------------------

        // Register for CrashedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, broadcast -> {
            terminate();
            // cheak this
            boolean isError = true; // Set to true if an error occurred
            String errorDescription = broadcast.getErrorMessage(); // Populate if isError = true
            String faultySensor = broadcast.getSenderId(); // Populate if isError = true
            Map<String, Object> lastFrames = new HashMap<>(); // Populate if isError = true
            int currentTick = fusionSlam.getCurrentTick();
            List<Pose> poses = fusionSlam.getPosesUpToTick(currentTick); // קבלת כל ה-Pose עד לנקודת השגיאה
            fusionSlam.generateOutputFile("output_file.json", isError, errorDescription, faultySensor, lastFrames,
                    poses);// איפה הקובץ?

        });
    }
}