package bgu.spl.mics.application;

import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * The main entry point for the GurionRock Pro Max Ultra Over 9000 simulation.
 * <p>
 * This class initializes the system and starts the simulation by setting up
 * services, objects, and configurations.
 * </p>
 */
public class GurionRockRunner {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java GurionRockRunner <config-file-path>");
            return;
        }

        String configFilePath = args[0];
        File configFile = new File(configFilePath);

        if (!configFile.exists()) {
            System.out.println("Configuration file not found at: " + configFilePath);
            return;
        }

        // Get the parent directory of the config file
        File configDirectory = configFile.getParentFile();

        try (FileReader reader = new FileReader(configFilePath)) {
            Gson gson = new Gson();
            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();

            // Print loaded configuration for debugging
            System.out.println("Configuration loaded: " + config);

            // Parse Cameras
            JsonArray camerasConfig = config.getAsJsonArray("Cameras");
            List<Thread> cameraThreads = new ArrayList<>();
            Set<Integer> cameraIds = new HashSet<>();

            for (JsonElement cameraElement : camerasConfig) {
                if (!cameraElement.isJsonObject()) {
                    continue; // Skip null or invalid elements
                }

                JsonObject camera = cameraElement.getAsJsonObject();
                int id = camera.get("id").getAsInt();
                if (!cameraIds.add(id)) {
                    throw new IllegalArgumentException("Duplicate camera ID detected: " + id);
                }
                int frequency = camera.get("frequency").getAsInt();
                String key = camera.get("camera_key").getAsString();
                String cameraDataPath = camera.get("camera_datas_path").getAsString();
                File cameraDataFile = new File(configDirectory, cameraDataPath); // Resolve relative path
                if (key == null || key.isEmpty()) {
                    throw new IllegalArgumentException("camera_key is missing or invalid for camera ID: " + id);
                }
                Camera cam = new Camera(id, frequency, cameraDataFile.getAbsolutePath(), key); // Use absolute path
                CameraService cameraService = new CameraService(cam);
                cameraThreads.add(new Thread(cameraService, "CameraService" + id));
            }

            // Parse LiDars
            JsonObject lidarsConfig = config.getAsJsonObject("Lidars");
            String lidarDataPath = lidarsConfig.get("lidars_data_path").getAsString();
            File lidarDataFile = new File(configDirectory, lidarDataPath); // Resolve relative path

            List<JsonObject> lidarsList = gson.fromJson(
                    lidarsConfig.getAsJsonArray("LidarConfigurations"),
                    new TypeToken<List<JsonObject>>() {
                    }.getType());

            List<Thread> lidarThreads = new ArrayList<>();
            Set<Integer> lidarIds = new HashSet<>();

            for (JsonObject lidar : lidarsList) {
                int id = lidar.get("id").getAsInt();
                if (!lidarIds.add(id)) {
                    throw new IllegalArgumentException("Duplicate LiDar ID detected: " + id);
                }
                int frequency = lidar.get("frequency").getAsInt();
                LiDarWorkerTracker lidarTracker = new LiDarWorkerTracker(id, frequency,
                        lidarDataFile.getAbsolutePath()); // Use absolute path
                LiDarService lidarService = new LiDarService("LiDarService" + id, lidarTracker);
                lidarThreads.add(new Thread(lidarService, "LiDarService" + id));
            }

            // Parse Pose data
            String poseDataPath = config.get("poseJsonFile").getAsString();
            File poseDataFile = new File(configDirectory, poseDataPath); // Resolve relative path
            GPSIMU gpsimu = new GPSIMU(poseDataFile.getAbsolutePath()); // Use absolute path
            PoseService poseService = new PoseService(gpsimu);
            Thread poseThread = new Thread(poseService, "PoseService");

            // Parse Time configuration
            int tickTime = config.get("TickTime").getAsInt();
            int duration = config.get("Duration").getAsInt();
            TimeService timeService = new TimeService(tickTime, duration);
            Thread timeThread = new Thread(timeService, "TimeService");

            // Initialize FusionSlam
            FusionSlam fusionSlam = FusionSlam.getInstance();
            FusionSlamService fusionSlamService = new FusionSlamService(fusionSlam);
            Thread fusionSlamThread = new Thread(fusionSlamService, "FusionSlamService");

            // Update FusionSlam with service count
            fusionSlam.setserviceCounter(
                    cameraThreads.size() + lidarThreads.size() + 1 // +1 for PoseService
            );

            // Start all threads except TimeService
            cameraThreads.forEach(Thread::start);
            lidarThreads.forEach(Thread::start);
            poseThread.start();
            fusionSlamThread.start();

            // Sleep briefly to allow all threads to register
            try {
                Thread.sleep(100); // Adjust this if needed to ensure registration
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Start TimeService last
            timeThread.start();

            // Wait for TimeService to complete
            try {
                timeThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("Simulation completed successfully!");

        } catch (IOException e) {
            System.out.println("Error reading configuration file: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.out.println("Configuration error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
