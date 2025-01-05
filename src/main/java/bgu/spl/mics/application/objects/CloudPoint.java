package bgu.spl.mics.application.objects;

/**
 * CloudPoint represents a specific point in a 2D space as detected by the
 * LiDAR.
 * These points are used to generate a point cloud representing objects in the
 * environment.
 */
// cloud point class
public class CloudPoint {

    private double x;
    private double y;

    public CloudPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return String.format("{\"x\":%.2f,\"y\":%.2f}", x, y);
    }

}