package bgu.spl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObject;

import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;

class CameraTest {

    private Camera camera;

    @BeforeEach
    void setup() {
        java.net.URL resource = getClass().getClassLoader().getResource("camera_example_input.json");
        assertNotNull(resource, "The file 'example_input.json' was not found in src/test/resources.");
        System.out.println("File found: " + resource.getPath());

        try {
            String filePath = Paths.get(resource.toURI()).toString();
            camera = new Camera(1, 5, filePath, "camera1");
        } catch (Exception e) {
            fail("Failed to initialize camera: " + e.getMessage());
        }
    }

    @Test
    void testGetDetectedObjectsAtValidTimeWithoutError() {
        // בדיקה של זמן שבו אין אובייקט עם שגיאה (זמן 13)
        StampedDetectedObject result = camera.getDetectedObjectsAtTime(13);
        assertNotNull(result, "The result should not be null for valid time.");
        assertEquals(13, result.getTime(), "The returned time should match the requested time.");
        assertEquals(2, result.getDetectedObjects().size(), "The detected objects list size should be correct.");
        assertEquals("Wall_1", result.getDetectedObjects().get(0).getId(), "The object ID should match.");
        assertEquals("Wall", result.getDetectedObjects().get(0).getDescription(),
                "The object description should match.");
    }

    @Test
    void testGetDetectedObjectsAtValidTimeWithError() {
        // בדיקה של זמן שבו יש אובייקט עם שגיאה (זמן 8)
        StampedDetectedObject result = camera.getDetectedObjectsAtTime(8);
        assertNotNull(result, "The result should not be null for valid time.");
        assertEquals(8, result.getTime(), "The returned time should match the requested time.");
        assertEquals(3, result.getDetectedObjects().size(), "The detected objects list size should be correct.");

        // בדיקת שינוי הסטטוס
        assertEquals(STATUS.ERROR, camera.getStatus(), "Camera status should be set to ERROR.");
        assertEquals(
                "GLaDOS has repurposed the robot to conduct endless cake-fetching tests. Success is a lie.",
                camera.getErrMString(),
                "Error message string should match the detected object's description.");
    }

    @Test
    void testGetDetectedObjectsAtInvalidTime() {
        // בדיקה של זמן שלא קיים ברשימה
        StampedDetectedObject result = camera.getDetectedObjectsAtTime(20);
        assertNull(result, "The result should be null for a time with no detected objects.");
    }

    @Test
    void testCheckIfDoneSetsStatusToDown() {
        // בדיקה של שינוי הסטטוס לאחר זמן שעבר את maxTime
        camera.getDetectedObjectsAtTime(12); // זמן תקין
        assertEquals(STATUS.UP, camera.getStatus(), "for time 12, Camera status should remain UP for valid time.");

        camera.getDetectedObjectsAtTime(20); // זמן שחורג מה-mTime
        assertEquals(STATUS.DOWN, camera.getStatus(),
                "for time 20, Camera status should be set to DOWN after maxTime.");
    }
}