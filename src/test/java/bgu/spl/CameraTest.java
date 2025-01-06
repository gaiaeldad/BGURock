package bgu.spl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObject;
import bgu.spl.mics.application.objects.DetectedObject;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CameraTest {

    private Camera camera;

    @BeforeEach
    void initCamera() {

        List<StampedDetectedObject> objects = new ArrayList<>();

        // adding objects
        objects.add(new StampedDetectedObject(15, List.of(
                new DetectedObject("Object_A", "Metallic Barrier"),
                new DetectedObject("Sign_5", "Caution Sign"))));
        objects.add(new StampedDetectedObject(10, List.of(
                new DetectedObject("Chair_2", "Plastic Chair"),
                new DetectedObject("ERROR", "Malfunction in the detection system. Restart required."),
                new DetectedObject("Table_4", "Wooden Table"))));
        objects.add(new StampedDetectedObject(5, List.of()));

        camera = new Camera(3, 8, objects, 15);
    }

    @Test
    void testRetrieveValidObjectsAtTime() {
        StampedDetectedObject result = camera.getDetectedObjectsAtTime(15);
        assertNotNull(result, "Result should not be null for a valid time.");
        assertEquals(15, result.getTime(), "Returned time should match the requested time.");
        assertEquals(2, result.getDetectedObjects().size(), "Detected objects count should be correct.");
        assertEquals("Object_A", result.getDetectedObjects().get(0).getId(), "Object ID should match.");
    }

    @Test
    void testRetrieveObjectsWithError() {
        StampedDetectedObject result = camera.getDetectedObjectsAtTime(10);
        assertNotNull(result, "Result should not be null for valid time.");
        assertEquals(10, result.getTime(), "Returned time should match the requested time.");
        assertEquals(3, result.getDetectedObjects().size(), "Detected objects count should be correct.");
        assertEquals(STATUS.ERROR, camera.getStatus(), "Camera status should be ERROR due to detected error.");
        assertEquals(
                "Malfunction in the detection system. Restart required.",
                camera.getErrMString(),
                "Error message should match the problematic object's description.");
    }

    @Test
    void testRetrieveObjectsInvalidTime() {
        StampedDetectedObject result = camera.getDetectedObjectsAtTime(20);
        assertNull(result, "Result should be null for a time with no objects.");
    }

    @Test
    void testUpdateStatusToDownAfterMaxTime() {
        camera.getDetectedObjectsAtTime(5);
        assertEquals(STATUS.UP, camera.getStatus(), "Camera should remain UP for valid time.");
        camera.getDetectedObjectsAtTime(20);
        assertEquals(STATUS.DOWN, camera.getStatus(), "Camera should switch to DOWN after exceeding max time.");
    }

    @Test
    void testEmptyObjectsList() {
        Camera emptyCamera = new Camera(4, 9, new ArrayList<>(), 0); // maxTime = 0
        assertTrue(emptyCamera.getDetectedObjectsList().isEmpty(),
                "Objects list should be empty for uninitialized camera.");
    }

    @Test
    void testCameraWithSpecificDataKey() {
        List<StampedDetectedObject> dataForCamera1 = List.of(
                new StampedDetectedObject(6, List.of(
                        new DetectedObject("Sign_7", "Yield Sign"))));

        Camera camera1 = new Camera(5, 10, dataForCamera1, 6);
        StampedDetectedObject result = camera1.getDetectedObjectsAtTime(6);
        assertNotNull(result, "Objects for Camera 1 should not be null.");
        assertEquals("Sign_7", result.getDetectedObjects().get(0).getId(), "Object ID should match for Camera 1.");
    }

    @Test
    void testEmptyObjectsAtSpecificTime() {
        List<StampedDetectedObject> objects = List.of(
                new StampedDetectedObject(5, new ArrayList<>()));

        Camera testCamera = new Camera(6, 12, objects, 5);
        StampedDetectedObject result = testCamera.getDetectedObjectsAtTime(5);
        assertNotNull(result, "Result should not be null even if there are no objects.");
        assertTrue(result.getDetectedObjects().isEmpty(), "Objects list should be empty for time 5.");
    }

    @Test
    void testCameraFrequencyInitialization() {
        assertEquals(8, camera.getFrequency(), "Camera frequency should be initialized correctly.");
    }
}
