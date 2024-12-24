package bgu.spl.mics.application.objects;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase {

    // Singleton instance
    private static LiDarDataBase instance;
    
    // List to hold all the cloud points from different objects at different times
    private List<StampedCloudPoints> cloudPoints;

    // Private constructor to prevent instantiation from outside
    private LiDarDataBase(String filePath) {
        this.cloudPoints = new ArrayList<>();
        loadDataFromFile(filePath);
    }

    /**
     * Returns the singleton instance of LiDarDataBase.
     *
     * @param filePath The path to the LiDAR data file.
     * @return The singleton instance of LiDarDataBase.
     */
    public static LiDarDataBase getInstance(String filePath) {
        if (instance == null) {
            synchronized (LiDarDataBase.class) {
                if (instance == null) {
                    instance = new LiDarDataBase(filePath);
                }
            }
        }
        return instance;
    }

    /**
     * Loads the data from a file to populate the cloudPoints list.
     * Assumes the file format is CSV-like with the object ID, time, and coordinates.
     *
     * @param filePath The path to the LiDAR data file.
     */
//לוודא!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    private void loadDataFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Split the line by commas
                String[] parts = line.split(",");

                // Check if the line has the correct number of parts (object ID, time, and cloud points)
                if (parts.length < 3) {
                    System.out.println("Skipping invalid line: " + line);
                    continue;
                }

                // Get the object ID and time
                String objectId = parts[0].trim();
                int time = Integer.parseInt(parts[1].trim());

                // Parse the cloud points (x, y pairs)
                List<Double> cloudPointList = new ArrayList<>();
                for (int i = 2; i < parts.length; i++) {
                    cloudPointList.add(Double.parseDouble(parts[i].trim()));
                }

                // Create a StampedCloudPoints object and add it to the list
                StampedCloudPoints stampedCloudPoints = new StampedCloudPoints(objectId, time, cloudPointList);
                cloudPoints.add(stampedCloudPoints);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a StampedCloudPoints to the LiDar database.
     *
     * @param stampedCloudPoints The StampedCloudPoints object to be added.
     */
    public void addCloudPoints(StampedCloudPoints stampedCloudPoints) {
        cloudPoints.add(stampedCloudPoints);
    }

    /**
     * Returns the list of StampedCloudPoints.
     *
     * @return The list of StampedCloudPoints.
     */
    public List<StampedCloudPoints> getCloudPoints() {
        return cloudPoints;
    }
}