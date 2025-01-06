package bgu.spl.mics.application.objects;

import java.io.File;
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
    // GPSIMU class

    private int currTick;
    private STATUS status;
    private List<Pose> poseList;
    private int maxTime;

    public GPSIMU(List<Pose> poseList, int maxTime) { // Constructor for main class
        this.currTick = 0;
        this.status = STATUS.UP; // Default status
        this.poseList = poseList;
        this.maxTime = maxTime; // Calculate the maximum time
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public STATUS getStatus() {
        return status;
    }

    public void SetTick(int time) {
        currTick = time;

    }

    public int getCurrentTick() {
        return currTick;
    }

    public List<Pose> getPoseList() {
        return poseList;
    }

    public Pose getPoseAtTime() {
        updateStatusBasedOnTime();
        for (Pose pose : poseList) {
            if (pose.getTime() == this.currTick) {
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
            System.out.println("pose attempting to read file: " + new File(filePath).getAbsolutePath());
            Gson gson = new Gson();
            List<Pose> data = gson.fromJson(reader, new TypeToken<List<Pose>>() {
            }.getType());
            System.out.println("pose loaded " + data.size() + " detected objects.");
            return data;
        } catch (IOException e) {
            return new ArrayList<>(); // Return an empty list in case of failure
        }
    }

    public void updateStatusBasedOnTime() {
        if (currTick >= maxTime) {
            System.out.println("pose down because time is: " + getCurrentTick());
            setStatus(STATUS.DOWN);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GPSIMU{currentTick=").append(currTick)
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