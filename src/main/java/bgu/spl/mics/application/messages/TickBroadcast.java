package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class TickBroadcast implements Broadcast {
    private final int time;
    private String senderId;

    public TickBroadcast(int time,String senderId) {
        this.time = time;
        this.senderId = senderId;
    }

    public int getTime() {
        return time;
    }
    public String getSenderId() {
        return senderId;
    }
}