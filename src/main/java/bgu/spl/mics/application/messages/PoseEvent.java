package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.Pose;

/**
 * An event that represents the pose of the robot.
 */
// PoseEvent class
public class PoseEvent implements Event<Boolean> {
    private String senderName;
    private final Pose pose;

    public PoseEvent(Pose pose, String senderName) {
        this.pose = pose;
        this.senderName = senderName;
    }

    public String getSenderName() {
        return senderName;
    }

    public Pose getPose() {
        return pose;
    }

}