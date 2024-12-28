package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class TickBroadcast implements Broadcast {
    private final int time; // The current time
    private final int finalTick; // The final tick of the simulation

    public TickBroadcast(int time, int finalTick) {
        this.time = time;
        this.finalTick = finalTick;
    }

    public int getTime() {
        return time;
    }

    public int getFinalTick() {
        return finalTick;
    }

    public boolean isFinalTick() {
        return time >= finalTick; // Return true if the current time is the last tick or beyond
    }
}
