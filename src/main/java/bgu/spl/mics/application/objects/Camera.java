package bgu.spl.mics.application.objects;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 * Invariants:
 * - detectedObjectsList is never null.
 * - status is always one of the STATUS enum values (UP, DOWN, ERROR).
 * - maxTime is non-negative.
 */
public class Camera {

    private int id;
    private int frequency;
    private STATUS status;
    private List<StampedDetectedObject> detectedObjectsList;
    private int maxTime;
    private String errMString;

    /**
     * Constructs a Camera object with specified parameters.
     * Preconditions:
     * - id >= 0
     * - frequency >= 0
     * - maxTime >= 0
     * - detectedObjectsList is not null
     * Postconditions:
     * - Camera object is initialized with given values.
     * - Status is set to UP.
     *
     * @param id                  the unique identifier for the camera
     * @param frequency           the frequency of operation
     * @param detectedObjectsList the list of detected objects with timestamps
     * @param maxTime             the maximum time allowed for data capture
     */

    public Camera(int id, int frequency, List<StampedDetectedObject> detectedObjectsList, int maxTime) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.detectedObjectsList = detectedObjectsList != null
                ? Collections.unmodifiableList(detectedObjectsList)
                : Collections.emptyList();
        this.maxTime = maxTime;
        this.errMString = null;
    }

    /**
     * Constructs a Camera object by loading detected objects from a file.
     * Preconditions:
     * - id >= 0
     * - frequency > 0
     * - filePath and cameraKey are valid (non-null, non-empty).
     * Postconditions:
     * - Camera object is initialized with loaded data or empty list if an error
     * occurs.
     *
     * @param id        the unique identifier for the camera
     * @param frequency the frequency of operation
     * @param filePath  path to the data file
     * @param cameraKey key to access the specific camera data in the file
     */

    public Camera(int id, int frequency, String filePath, String cameraKey) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.errMString = null;
        this.detectedObjectsList = new ArrayList<>();
        loadDetectedObjectsFromFile(filePath, cameraKey);
        if (!detectedObjectsList.isEmpty()) {
            this.maxTime = detectedObjectsList.stream()
                    .mapToInt(StampedDetectedObject::getTime)
                    .max()
                    .orElse(0);
        } else {
            this.maxTime = 0;
        }
    }

    /**
     * Retrieves the list of detected objects.
     * Postconditions:
     * - Returns a non-null list of detected objects.
     *
     * @return the list of detected objects
     */
    public List<StampedDetectedObject> getDetectedObjectsList() {
        return detectedObjectsList;
    }

    /**
     * Retrieves detected objects at a specific time.
     * Preconditions:
     * - time >= 0
     * Postconditions:
     * - Returns detected objects if available; null otherwise.
     * - Updates status to ERROR if an error object is detected.
     *
     * @param time the time to query detected objects
     * @return the detected objects at the specified time
     */

    public StampedDetectedObject getDetectedObjectsAtTime(int time) {
        checkIfDone(time);
        for (StampedDetectedObject stampedObject : detectedObjectsList) {
            if (stampedObject.getTime() == time) {
                for (DetectedObject obj : stampedObject.getDetectedObjects()) {
                    if ("ERROR".equals(obj.getId())) {
                        errMString = obj.getDescription();
                        setStatus(STATUS.ERROR);
                        break;
                    }
                }
                return stampedObject;
            }
        }
        return null;
    }

    /**
     * Loads detected objects from a specified file.
     * Preconditions:
     * - filePath and cameraKey are non-null and non-empty.
     * Postconditions:
     * - detectedObjectsList is updated based on the file data.
     * - maxTime is set based on the maximum timestamp in the data.
     * - If an error occurs, detectedObjectsList is set to an empty list.
     *
     * @param filePath  Path to the file containing detected objects.
     * @param cameraKey Key for retrieving specific camera data.
     */

    public void loadDetectedObjectsFromFile(String filePath, String cameraKey) {
        try (FileReader reader = new FileReader(filePath)) {
            System.out.println("Camera attempting to read file: " + new File(filePath).getAbsolutePath());
            Gson gson = new Gson();
            java.lang.reflect.Type type = new TypeToken<Map<String, List<List<StampedDetectedObject>>>>() {
            }.getType();
            Map<String, List<List<StampedDetectedObject>>> cameraData = gson.fromJson(reader, type);
            List<List<StampedDetectedObject>> nestedCameraObjects = cameraData.get(cameraKey);
            if (nestedCameraObjects != null) {
                List<StampedDetectedObject> cameraObjects = new ArrayList<>();
                for (List<StampedDetectedObject> list : nestedCameraObjects) {
                    cameraObjects.addAll(list);
                }
                detectedObjectsList = new ArrayList<>(cameraObjects);
                maxTime = cameraObjects.stream().mapToInt(StampedDetectedObject::getTime).max().orElse(4);
            } else {
                detectedObjectsList = new ArrayList<>();
            }
            System.out.println("Camera " + id + " loaded " + detectedObjectsList.size() + " detected objects.");
        } catch (IOException e) {
            detectedObjectsList = new ArrayList<>();
        } catch (Exception e) {
            detectedObjectsList = new ArrayList<>();
        }
    }

    /**
     * Checks if the camera has exceeded its maximum operational time.
     * Preconditions:
     * - currentTime >= 0
     * Postconditions:
     * - Status is updated to DOWN if the time exceeds maxTime.
     *
     * @param currentTime the current time to check
     */

    public void checkIfDone(int currentTime) {
        if (currentTime >= maxTime) {
            setStatus(STATUS.DOWN);
        }
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getFrequency() {
        return frequency;
    }

    public STATUS getStatus() {
        return status;
    }

    public String getErrMString() {
        return errMString;
    }
}