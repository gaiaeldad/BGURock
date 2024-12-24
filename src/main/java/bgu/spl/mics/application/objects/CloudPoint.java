package bgu.spl.mics.application.objects;

/**
 * CloudPoint represents a specific point in a 2D space as detected by the LiDAR.
 * These points are used to generate a point cloud representing objects in the environment.
 */
public class CloudPoint {

    private int x;  
    private int y;  

    public CloudPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Getters and Setters for x, y coordinates

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        return "CloudPoint{x=" + x + ", y=" + y + "}";
    }
/* 
    public void setX(int x) {
        this.x = x;
    }
    public void setY(int y) {
        this.y = y;
    }
*/
}