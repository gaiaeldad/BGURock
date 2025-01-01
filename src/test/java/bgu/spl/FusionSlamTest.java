package bgu.spl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.reflect.Method;

import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.CloudPoint;
import bgu.spl.mics.application.objects.TrackedObject;
import bgu.spl.mics.application.objects.LandMark;

import static org.junit.jupiter.api.Assertions.*;

class FusionSlamTest {

    private FusionSlam fusionSlam;

    @BeforeEach
    void setUp() {
        fusionSlam = FusionSlam.getInstance();
    }

    @Test
    void testSingleton() {
        FusionSlam instance1 = FusionSlam.getInstance();
        FusionSlam instance2 = FusionSlam.getInstance();
        assertSame(instance1, instance2, "FusionSlam should be a singleton");
    }

    @Test
    void testAddPoseAndGetPoseAtTime() {
        Pose pose1 = new Pose(1, 0, 0, 0);
        Pose pose2 = new Pose(2, 5, 5, 90);

        fusionSlam.addPose(pose1);
        fusionSlam.addPose(pose2);

        assertEquals(pose1, fusionSlam.getPoseAtTime(1), "Pose at time 1 should match");
        assertEquals(pose2, fusionSlam.getPoseAtTime(2), "Pose at time 2 should match");
    }

    @Test
    void testTransformToGlobal() throws Exception {
        Pose pose = new Pose(1, 5, 5, 90);
        List<CloudPoint> localPoints = Arrays.asList(new CloudPoint(1, 1), new CloudPoint(2, 2));

        // Access the private method using reflection
        Method method = FusionSlam.class.getDeclaredMethod("transformToGlobal", List.class, Pose.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<CloudPoint> globalPoints = (List<CloudPoint>) method.invoke(fusionSlam, localPoints, pose);

        assertNotNull(globalPoints, "Global points should not be null");
        assertEquals(2, globalPoints.size(), "Global points size should match local points size");

        // Verify specific transformations
        assertEquals(4.0, globalPoints.get(0).getX(), 0.01, "X coordinate transformation incorrect");
        assertEquals(6.0, globalPoints.get(0).getY(), 0.01, "Y coordinate transformation incorrect");
    }

    @Test
    void testProcessTrackedObjects_NewLandmark() {
        Pose pose = new Pose(1, 0, 0, 0);
        fusionSlam.addPose(pose);

        TrackedObject trackedObject = new TrackedObject("ID1", 1, "Wall", Arrays.asList(new CloudPoint(1, 1)));
        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject));

        List<LandMark> landmarks = fusionSlam.getLandmarks();
        assertEquals(1, landmarks.size(), "One landmark should have been added");
        assertEquals("ID1", landmarks.get(0).getId(), "Landmark ID should match");
    }

    @Test
    void testProcessTrackedObjects_UpdateLandmark() {
        Pose pose = new Pose(1, 0, 0, 0);
        fusionSlam.addPose(pose);

        TrackedObject trackedObject1 = new TrackedObject("ID1", 1, "Wall", Arrays.asList(new CloudPoint(1, 1)));
        TrackedObject trackedObject2 = new TrackedObject("ID1", 1, "Wall", Arrays.asList(new CloudPoint(2, 2)));

        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject1));
        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject2));

        List<LandMark> landmarks = fusionSlam.getLandmarks();
        assertEquals(1, landmarks.size(), "Only one landmark should exist");

        List<CloudPoint> updatedCoordinates = landmarks.get(0).getCoordinates();
        assertEquals(1.5, updatedCoordinates.get(0).getX(), 0.01, "X coordinate should be averaged");
        assertEquals(1.5, updatedCoordinates.get(0).getY(), 0.01, "Y coordinate should be averaged");
    }

    @Test
    void testGenerateOutputFile() {
        fusionSlam.addPose(new Pose(1, 0, 0, 0));

        TrackedObject trackedObject = new TrackedObject("ID1", 1, "Wall", Arrays.asList(new CloudPoint(1, 1)));
        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject));

        String filePath = "output_test.json";
        fusionSlam.generateOutputFile(filePath, false, null, null, new java.util.HashMap<>(), new ArrayList<>());

        // Verify the file exists and contains the expected data (this is optional and
        // depends on your test environment)
        java.io.File file = new java.io.File(filePath);
        assertTrue(file.exists(), "Output file should be created");

        // Clean up
        file.delete();
    }

    @Test
    void testServiceCounterManagement() {
        fusionSlam.setserviceCounter(2);
        fusionSlam.decreaseServiceCounter();
        assertEquals(1, fusionSlam.getserviceCounter(), "Service counter should decrease by 1");

        fusionSlam.increasServiceCounter();
        assertEquals(2, fusionSlam.getserviceCounter(), "Service counter should increase by 1");
    }
}
