package bgu.spl.mics.application.objects;

import java.util.List;

/**
 * Represents the robot's GPS and IMU system.
 * Provides information about the robot's position and movement.
 */
public class GPSIMU {

    // Fields
    private int currentTick;  // The current time (tick)
    private STATUS status;  // The status of the system (UP, DOWN, ERROR)
    private List<Pose> poseList;  // List of time-stamped poses

    // Constructor to initialize the GPSIMU object
    public GPSIMU(int currentTick, STATUS status, List<Pose> poseList) {
        this.currentTick = currentTick;
        this.status = status;
        this.poseList = poseList;
    }

    // Get the current time (tick)
    public int getCurrentTick() {
        return currentTick;
    }


    // Get the status of the GPSIMU system
    public STATUS getStatus() {
        return status;
    }

    // Get the list of poses
    public List<Pose> getPoseList() {
        return poseList;
    }

    // Add a pose to the pose list
    public void addPose(Pose pose) {
        this.poseList.add(pose);
    }
    
    // Optional: Override toString() to provide a string representation of the GPSIMU object
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GPSIMU{currentTick=").append(currentTick)
          .append(", status=").append(status)
          .append(", poseList=");
        
        // Print all poses
        for (Pose pose : poseList) {
            sb.append(pose).append(", ");
        }
        sb.append("}");
        return sb.toString();
    }
/* 
    public void setCurrentTick(int currentTick) {
        this.currentTick = currentTick;
    }
    public void setPoseList(List<Pose> poseList) {
        this.poseList = poseList;
    }
    public void setStatus(STATUS status) {
        this.status = status;
    }
    public void setStatus(String statusString) {
        this.status = STATUS.fromString(statusString);  // Use the STATUS enum to convert string to enum
    }
*/
}