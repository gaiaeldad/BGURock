package bgu.spl.mics.application.objects;

/**
 * Represents the robot's pose (position and orientation) in the environment.
 * Includes x, y coordinates and the yaw angle relative to a global coordinate
 * system.
 */
// pose class
public class Pose {

    private int time;
    private float x;
    private float y;
    private float yaw;

    // Constructor to initialize the Pose object
    public Pose(int time, float x, float y, float yaw) {
        this.time = time;
        this.x = x;
        this.y = y;
        this.yaw = yaw;
    }

    // Getters
    public int getTime() {
        return time;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getYaw() {
        return yaw;
    }

    @Override
    public String toString() {
        return String.format("{\"time\":%d,\"x\":%.2f,\"y\":%.2f,\"yaw\":%.2f}", time, x, y, yaw);
    }
}