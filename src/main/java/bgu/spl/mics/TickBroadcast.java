package bgu.spl.mics;

public class TickBroadcast implements Broadcast {
    private int time;

    public TickBroadcast(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }
}
