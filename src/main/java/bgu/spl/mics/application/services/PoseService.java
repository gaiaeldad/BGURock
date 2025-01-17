package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.STATUS;

/**
 * PoseService is responsible for maintaining the robot's current pose (position
 * and orientation)
 * and broadcasting PoseEvents at every tick.
 */
// p
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
     * Subscribes to TickBroadcast and sends PoseEvents at every tick based on the
     * current pose.
     */

    @Override
    protected void initialize() {
        // Subscribe to TickBroadcast to handle ticks
        subscribeBroadcast(TickBroadcast.class, tick -> {
            gpsimu.SetTick(tick.getTime());
            System.out.println(getName() + ": recived tickBrodckast, tick: " + tick.getTime());
            if (gpsimu.getStatus() == STATUS.UP) {
                Pose currentPose = gpsimu.getPoseAtTime();
                if (currentPose != null) {
                    // Broadcast PoseEvent
                    System.out.println(getName() + ": sent a pose event, time: " + tick.getTime());
                    sendEvent(new PoseEvent(currentPose, getName()));
                }
                if (gpsimu.getStatus() == STATUS.DOWN) {
                    System.out.println(getName() + ": is terminated because GPSIMU is down");
                    terminate();
                    sendBroadcast(new TerminatedBroadcast(getName()));
                }
            } else {
                System.out.println(getName() + ": is terminated");
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));
            }
        });

        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast broadcast) -> {
            if ("TimeService".equals(broadcast.getSenderName())) {
                System.out.println(getName() + ": recived TerminatedBroadcast from TimeService");
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));
            }
        });

        // Subscribe to CrashedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast broadcast) -> {
            System.out.println(getName() + ": recived CrashedBroadcast from " + broadcast.getSenderName());
            terminate();
        });
    }

}