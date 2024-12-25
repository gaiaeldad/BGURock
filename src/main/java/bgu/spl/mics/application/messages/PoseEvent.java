package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.Pose;

public class PoseEvent implements Event<Void> {
    private String senderName;
    private final Pose pose;

    public PoseEvent(Pose pose,String senderName) {
        this.pose = pose;
        this.senderName = senderName;
    }
    public Pose getPose() {
        return pose;
    }
    public String getSenderName() {
        return senderName;
    }
}