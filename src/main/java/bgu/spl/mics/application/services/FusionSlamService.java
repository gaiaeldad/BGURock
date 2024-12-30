package bgu.spl.mics.application.services;

import bgu.spl.mics.*;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.messages.*;

/**
 * FusionSlamService integrates data from multiple sensors to build and update
 * the robot's global map.
 */
public class FusionSlamService extends MicroService {
    private final FusionSlam fusionSlam;

    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */
    public FusionSlamService(FusionSlam fusionSlam) {
        super("FusionSlamService");
        this.fusionSlam = FusionSlam.getInstance();    }

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle events and broadcasts.
     */
    @Override
    protected void initialize() {
        // Register for TrackedObjectsEvent
        subscribeEvent(TrackedObjectsEvent.class, event -> {
            fusionSlam.processTrackedObjects(event.getTrackedObjects());
            complete(event, true);
        });

        // Register for PoseEvent
        subscribeEvent(PoseEvent.class, event -> {
            fusionSlam.addPose(event.getPose());
            complete(event, true);
        });

        // Register for TickBroadcast
        subscribeBroadcast(TickBroadcast.class, broadcast -> {
            if (broadcast.isFinalTick()) {
                terminate();
            }
            ////-------לבדוק אם צריך לעדכן זמנים כי פיוזן סלאם לא מתשתשת בזמן ולבדוק final tick
        });

        // Register for TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, broadcast -> {
            fusionSlam.decreaseServiceCounter();
            //------------צריך לקרות פה דברים
        });

        // Register for CrashedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, broadcast -> {
            terminate();
        });
    }
}