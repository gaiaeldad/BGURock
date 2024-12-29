package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * TimeService acts as the global timer for the system, broadcasting TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class TimeService extends MicroService {

    private final int tickTime;  // Duration of each tick in milliseconds
    private final int duration;  // Total number of ticks


    /**
     * Constructor for TimeService.
     *
     * @param tickTime  The duration of each tick in milliseconds.
     * @param duration  The total number of ticks before the service terminates.
     */
    public TimeService(int tickTime, int duration) {
        super("TimeService");
        this.tickTime = tickTime;
        this.duration = duration;
    }

    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified duration.
     */
    @Override
    protected void initialize() {
        System.out.println("TimeService initialized.");
        try {
            for (int currentTick = 1; currentTick <= duration && !FusionSlam.getInstance().isTerminated(); currentTick++) {
                // Broadcast the current tick
                sendBroadcast(new TickBroadcast(currentTick, duration));
                System.out.println("TimeService broadcasted Tick: " + currentTick);
                // Update system runtime in StatisticalFolder
                StatisticalFolder.getInstance().updateSystemRuntime(1);
                // Wait for the next tick
                Thread.sleep(tickTime * 1000L);
            }

            // After all ticks are complete, broadcast TerminatedBroadcast
            sendBroadcast(new TerminatedBroadcast(getName()));
            System.out.println("TimeService broadcasted TerminatedBroadcast.");
        } catch (InterruptedException e) {
            System.out.println("TimeService interrupted. Terminating...");
            Thread.currentThread().interrupt(); // Restore interrupt status
        } finally {
            terminate(); // Signal the service to terminate
        }
    }
}

