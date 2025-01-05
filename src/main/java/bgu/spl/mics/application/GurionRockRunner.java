package bgu.spl.mics.application;

import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * The main entry point for the GurionRock Pro Max Ultra Over 9000 simulation.
 * <p>
 * This class initializes the system and starts the simulation by setting up
 * services, objects, and configurations.
 * </p>
 */
public class GurionRockRunner {

    /**
     * The main method of the simulation.
     * This method sets up the necessary components, parses configuration files,
     * initializes services, and starts the simulation.
     *
     * @param args Command-line arguments. The first argument is expected to be the
     *             path to the configuration file.
     */
    // main method
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: Configuration file path is required as first argument.");
            return;
        }

        // Path to the configuration file provided as the first argument
        String configPath = args[0];
        File configFile = new File(configPath);
        String configDirectory = configFile.getParent(); // Extract the directory containing the config file

        try (FileReader reader = new FileReader(configPath)) {
            // Parse the configurationfile into a JsonObject
            Gson gson = new Gson();
            JsonObject config = gson.fromJson(reader, JsonObject.class);

            // Initialize the Cameras
            List<CameraService> cameraServices = new ArrayList<>();
            JsonObject camerasConfig = config.getAsJsonObject("Cameras");

            // Get the path to the camera data file and adjust it to be relative to the
            // config directory
            String cameraDataPath = camerasConfig.get("camera_datas_path").getAsString();
            cameraDataPath = Paths.get(configDirectory, cameraDataPath).toString();

            // Get camera configurations
            JsonArray cameraConfigs = camerasConfig.getAsJsonArray("CamerasConfigurations");

            // Parse camera data from the JSON file
            try (FileReader cameraDataReader = new FileReader(cameraDataPath)) {
                JsonObject cameraData = gson.fromJson(cameraDataReader, JsonObject.class);

                // Create CameraService for each camera configuration
                for (com.google.gson.JsonElement cameraConfig : cameraConfigs) {
                    JsonObject cameraJson = cameraConfig.getAsJsonObject();
                    int id = cameraJson.get("id").getAsInt();
                    int frequency = cameraJson.get("frequency").getAsInt();
                    String cameraKey = cameraJson.get("camera_key").getAsString();
                    // Retrieve stamped detected objects for this camera
                    JsonArray stampedObjectsJson = cameraData.getAsJsonArray(cameraKey);
                    List<StampedDetectedObject> detectedObjectsList = new ArrayList<>();

                    for (com.google.gson.JsonElement stampedObjectJson : stampedObjectsJson) {
                        JsonObject stampedObject = stampedObjectJson.getAsJsonObject();
                        int time = stampedObject.get("time").getAsInt();
                        JsonArray detectedObjectsJson = stampedObject.getAsJsonArray("detectedObjects");

                        List<DetectedObject> detectedObjects = new ArrayList<>();
                        for (com.google.gson.JsonElement detectedObjectJson : detectedObjectsJson) {
                            JsonObject detectedObject = detectedObjectJson.getAsJsonObject();
                            String idStr = detectedObject.get("id").getAsString();
                            String description = detectedObject.get("description").getAsString();
                            detectedObjects.add(new DetectedObject(idStr, description));
                        }
                        detectedObjectsList.add(new StampedDetectedObject(time, detectedObjects));
                    }
                    // Compute maxTime as the maximum time in the detectedObjectsList
                    int maxTime = detectedObjectsList.stream().mapToInt(StampedDetectedObject::getTime).max().orElse(0); // Default
                                                                                                                         // to
                                                                                                                         // 0
                                                                                                                         // if
                                                                                                                         // the
                                                                                                                         // list
                                                                                                                         // is
                                                                                                                         // empty

                    // Create Camera object with the computed maxTime
                    // Create Camera object and corresponding CameraService
                    Camera camera = new Camera(id, frequency, detectedObjectsList, maxTime);
                    cameraServices.add(new CameraService(camera));
                }
            }

            // Initialize LiDARs
            List<LiDarService> lidarServices = new ArrayList<>();
            JsonObject lidarConfig = config.getAsJsonObject("LiDarWorkers");

            // Get the path to the LiDAR data file and adjust it to be relative to the
            // config directory
            String lidarDataPath = lidarConfig.get("lidars_data_path").getAsString();
            lidarDataPath = Paths.get(configDirectory, lidarDataPath).toString();

            // Initialize the singleton instance of LiDarDataBase
            LiDarDataBase.getInstance(lidarDataPath);

            // Get LiDAR configurations
            JsonArray lidarConfigs = lidarConfig.getAsJsonArray("LidarConfigurations");

            // Create LiDarService for each LiDAR configuration
            for (com.google.gson.JsonElement lidarJson : lidarConfigs) {
                int id = lidarJson.getAsJsonObject().get("id").getAsInt();
                String name = "LiDarService" + id;
                int frequency = lidarJson.getAsJsonObject().get("frequency").getAsInt();
                int duration = config.get("Duration").getAsInt();
                LiDarWorkerTracker lidarWorker = new LiDarWorkerTracker(id, frequency, lidarDataPath, duration);
                lidarServices.add(new LiDarService(name, lidarWorker));
            }

            // Initialize PoseService
            String poseFilePath = config.get("poseJsonFile").getAsString();
            poseFilePath = Paths.get(configDirectory, poseFilePath).toString(); // Adjust path
            PoseService poseService;

            // Parse pose data from the JSON file
            try (FileReader poseReader = new FileReader(poseFilePath)) {
                java.lang.reflect.Type poseListType = new com.google.gson.reflect.TypeToken<List<Pose>>() {
                }.getType();
                List<Pose> poseList = gson.fromJson(poseReader, poseListType);
                // Compute maxTime as the maximum time in the poseList
                int maxTime = poseList.stream().mapToInt(Pose::getTime).max().orElse(0); // Default to 0 if the list is
                                                                                         // empty

                // Create GPSIMU and initialize PoseService
                GPSIMU gpsimu = new GPSIMU(poseList, maxTime);
                poseService = new PoseService(gpsimu);
            }

            // Initialize FusionSlamService
            FusionSlam fusionSlam = FusionSlam.getInstance();
            FusionSlamService fusionSlamService = new FusionSlamService(fusionSlam, configDirectory);

            // Count active cameras and sensors
            int numActiveCameras = cameraServices.size();
            int numActiveLiDars = lidarServices.size();
            int numActiveSensors = numActiveCameras + numActiveLiDars;

            // Update FusionSlam with active sensors and cameras
            fusionSlam.setActiveCameras(numActiveCameras);
            fusionSlam.setActiveSensors(numActiveSensors + 1);

            // Print debug information
            System.out.println("Active Cameras: " + numActiveCameras);
            System.out.println("Active Sensors: " + numActiveSensors);

            // Initialize simulation parameters
            int tickTime = config.get("TickTime").getAsInt();
            int duration = config.get("Duration").getAsInt();
            TimeService timeService = new TimeService(tickTime, duration);

            // Create threads for all services
            List<Thread> threads = new ArrayList<>();
            for (CameraService cameraService : cameraServices) {
                threads.add(new Thread(cameraService));
            }
            for (LiDarService lidarService : lidarServices) {
                threads.add(new Thread(lidarService));
            }
            threads.add(new Thread(poseService));
            threads.add(new Thread(fusionSlamService));

            // TimeService runs separately
            Thread timeServiceThread = new Thread(timeService);
            threads.add(timeServiceThread);

            // Start all threads except TimeService
            for (Thread thread : threads) {
                if (thread != timeServiceThread) {
                    thread.start();
                }
            }

            // Allow other services to initialize before starting TimeService
            Thread.sleep(100);
            timeServiceThread.start();

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

        } catch (IOException | InterruptedException e) {
            // Handle exceptions for file reading and thread interruptions
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}