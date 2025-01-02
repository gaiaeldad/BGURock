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
 * Primary entry point for the GurionRock Pro Max simulation framework.
 * This class sets up the environment, initializes components, and starts the
 * simulation.
 */
public class GurionRockRunner {

    /**
     * Launches the simulation based on the provided configuration file.
     *
     * @param args Command-line arguments. Requires the configuration file path as
     *             the first argument.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: Configuration file path is required.");
            return;
        }
        // load config
        String configFilePath = args[0];
        File configFile = new File(configFilePath);
        String configDirectory = configFile.getParent();

        // Opens the configuration file and parses it into a JsonObject using Gson.
        try (FileReader reader = new FileReader(configFilePath)) {
            Gson gson = new Gson();
            JsonObject config = gson.fromJson(reader, JsonObject.class);

            // Initialize Cameras
            List<CameraService> cameraServiceList = new ArrayList<>();
            JsonObject cameraSettings = config.getAsJsonObject("Cameras");

            String cameraDataPath = Paths.get(configDirectory, cameraSettings.get("camera_datas_path").getAsString())
                    .toString();
            JsonArray cameraConfigArray = cameraSettings.getAsJsonArray("CamerasConfigurations");

            try (FileReader camDataReader = new FileReader(cameraDataPath)) {
                JsonObject camData = gson.fromJson(camDataReader, JsonObject.class);

                for (com.google.gson.JsonElement camConfig : cameraConfigArray) {
                    JsonObject camDetails = camConfig.getAsJsonObject();
                    int id = camDetails.get("id").getAsInt();
                    int frequency = camDetails.get("frequency").getAsInt();
                    String key = camDetails.get("camera_key").getAsString();

                    Camera camera = new Camera(id, frequency, cameraDataPath, key);
                    cameraServiceList.add(new CameraService(camera));
                }
            }

            // Initialize LiDAR components
            List<LiDarService> lidarServiceList = new ArrayList<>();
            JsonObject lidarSettings = config.getAsJsonObject("LiDarWorkers");
            String lidarDataPath = Paths.get(configDirectory, lidarSettings.get("lidars_data_path").getAsString())
                    .toString();
            LiDarDataBase lidarDatabase = LiDarDataBase.getInstance(lidarDataPath);

            JsonArray lidarConfigArray = lidarSettings.getAsJsonArray("LidarConfigurations");
            for (com.google.gson.JsonElement lidarElement : lidarConfigArray) {
                JsonObject lidarDetails = lidarElement.getAsJsonObject();
                int id = lidarDetails.get("id").getAsInt();
                int frequency = lidarDetails.get("frequency").getAsInt();
                LiDarWorkerTracker lidarWorker = new LiDarWorkerTracker(id, frequency, lidarDataPath);
                lidarServiceList.add(new LiDarService("LiDarService" + id, lidarWorker));
            }

            // Initialize PoseService
            String poseFilePath = Paths.get(configDirectory, config.get("poseJsonFile").getAsString()).toString();
            PoseService poseService;

            try (FileReader poseReader = new FileReader(poseFilePath)) {
                List<Pose> poseData = gson.fromJson(poseReader, new com.google.gson.reflect.TypeToken<List<Pose>>() {
                }.getType());
                GPSIMU gpsimu = new GPSIMU(poseFilePath);
                poseService = new PoseService(gpsimu);
            }

            // Initialize Fusion slam
            FusionSlam fusionSlam = FusionSlam.getInstance();
            FusionSlamService fusionService = new FusionSlamService(fusionSlam);

            // Set the number of active sensors
            int totalCameras = cameraServiceList.size();
            int totalSensors = totalCameras + lidarServiceList.size();
            fusionSlam.setserviceCounter(totalSensors);

            System.out.println("Total Cameras: " + totalCameras);
            System.out.println("Total Sensors: " + totalSensors);

            // Timing Configuration
            int tickInterval = config.get("TickTime").getAsInt();
            int duration = config.get("Duration").getAsInt();
            TimeService timeService = new TimeService(tickInterval, duration);

            // Start threads for each service
            List<Thread> threads = new ArrayList<>();
            for (CameraService camService : cameraServiceList)
                threads.add(new Thread(camService));
            for (LiDarService lidarService : lidarServiceList)
                threads.add(new Thread(lidarService));
            threads.add(new Thread(poseService));
            threads.add(new Thread(fusionService));

            Thread timeThread = new Thread(timeService);
            // Start all threads and the time service

            for (Thread thread : threads)
                thread.start();
            Thread.sleep(100);
            timeThread.start();
            // Waits until all threads finish execution before ending the program.
            for (Thread thread : threads)
                thread.join();
            timeThread.join();

            // error handling
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
