package bgu.spl.mics.application.objects;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Represents the robot's GPS and IMU system.
 * Provides information about the robot's position and movement.
 */
public class GPSIMU {

    // Fields
    private int currentTick; 
    private STATUS status; 
    private List<Pose> poseList; 
    private int maxTime; 

    public GPSIMU(String filePath) {
        this.currentTick = 0;
        this.status = STATUS.UP;
        this.poseList = loadPosesFromFile(filePath); // Load poses directly into the list
        this.maxTime = calculateMaxTime(); // after this time the status needs to be DOWN
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public STATUS getStatus() {
        return status;
    }

    public List<Pose> getPoseList() {
        return poseList;
    }

    public Pose getPoseAtTime() {
        updateStatusBasedOnTime();
        for (Pose pose : poseList) {
            if (pose.getTime() == this.currentTick) {
                return pose;
            }
        }
        return null; 
    }
    public Pose getPoseAtTime(int time) {
        for (Pose pose : poseList) {
            if (pose.getTime() == time) {
                return pose;
            }
        }
        return null; 
    }

    public List<Pose> loadPosesFromFile(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, new TypeToken<List<Pose>>() {}.getType());
        } catch (IOException e) {
            return new ArrayList<>(); // Return an empty list in case of failure
        }
    }

    private int calculateMaxTime() {
        return poseList.stream().mapToInt(Pose::getTime).max().orElse(0);
    }

    
    //Update the status to DOWN if the current time exceeds or equals the maximum time.
    public void updateStatusBasedOnTime() {
        if (currentTick >= maxTime) {
            setStatus(STATUS.DOWN);
        }
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public void SetTick(int time) {
        currentTick = time;
        updateStatusBasedOnTime();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GPSIMU{currentTick=").append(currentTick)
          .append(", status=").append(status)
          .append(", maxTime=").append(maxTime)
          .append(", poseList=");
        
        for (Pose pose : poseList) {
            sb.append(pose).append(", ");
        }
        sb.append("}");
        return sb.toString();
    }
}