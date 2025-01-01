package bgu.spl.mics.application.objects;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */
public class Camera {

    private int id;
    private int frequency;
    private STATUS status;
    private List<StampedDetectedObject> detectedObjectsList;
    private int maxTime;
    private String errMString;

    public Camera(int id, int frequency, String filePath, String cameraKey) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        errMString = null;
        loadDetectedObjectsFromFile(filePath, cameraKey);
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

    public List<StampedDetectedObject> getDetectedObjectsList() {
        return detectedObjectsList;
    }

    public StampedDetectedObject getDetectedObjectsAtTime(int time) {
        checkIfDone(time);
        for (StampedDetectedObject stampedObject : detectedObjectsList) {
            if (stampedObject.getTime() == time) {
                for (DetectedObject obj : stampedObject.getDetectedObjects()) {
                    if ("ERROR".equals(obj.getId())) {
                        errMString = obj.getDescription();
                        setStatus(STATUS.ERROR);
                        return null;
                    }
                }
                return stampedObject;
            }
        }
        return null;
    }

    public void loadDetectedObjectsFromFile(String filePath, String cameraKey) {
        try (FileReader reader = new FileReader(filePath)) {
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
                maxTime = cameraObjects.stream().mapToInt(StampedDetectedObject::getTime).max().orElse(0);
            } else {
                detectedObjectsList = new ArrayList<>();
            }
        } catch (IOException e) {
            detectedObjectsList = new ArrayList<>();
        } catch (Exception e) {
            detectedObjectsList = new ArrayList<>();
        }
    }

    public void checkIfDone(int currentTime) {
        if (currentTime >= maxTime) {
            setStatus(STATUS.DOWN);
        }
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }
}