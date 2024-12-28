package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.STATUS;

/**
 * PoseService is responsible for maintaining the robot's current pose (position and orientation)
 * and broadcasting PoseEvents at every tick.
 */
public class PoseService extends MicroService {

    private final GPSIMU gpsimu;

    /**
     * Constructor for PoseService.
     *
     * @param gpsimu The GPSIMU object that provides the robot's pose data.
     */
    public PoseService(GPSIMU gpsimu) {
        super("PoseService");
        this.gpsimu = gpsimu;
    }

    /**
     * Initializes the PoseService.
     * Subscribes to TickBroadcast and sends PoseEvents at every tick based on the current pose.
     */
    @Override
    protected void initialize() {
        // Subscribe to TickBroadcast to handle ticks
        subscribeBroadcast(TickBroadcast.class, tick -> {
            gpsimu.SetTick(tick.getTime());
            if (gpsimu.getStatus() == STATUS.UP) {
                Pose currentPose = gpsimu.getPoseAtTime();

                if (currentPose != null) {
                    // Broadcast PoseEvent with the current pose and sender name
                    sendEvent(new PoseEvent(currentPose, getName()));
                }
            } else {
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));     
            }
        });
        //--------------------------------------לבדוק------------------------------------------------------------
        // Subscribe to TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast broadcast) -> {
            terminate();
        });
        // Subscribe to TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast broadcast) -> {
            terminate();// צריך לעשות גם לקראש
        });
    }
    
}