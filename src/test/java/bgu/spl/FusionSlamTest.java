package bgu.spl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import bgu.spl.mics.application.objects.CloudPoint;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.LandMark;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.TrackedObject;

import static org.junit.jupiter.api.Assertions.*;

class FusionSlamAdvancedTest {

    private FusionSlam fusionSlam;

    @BeforeEach
    void setUp() {
        fusionSlam = FusionSlam.getInstance();
    }

    @Test
    void testAddPoseAndRetrieve() {
        Pose pose1 = new Pose(1, 0, 0, 0);
        Pose pose2 = new Pose(2, 5, 5, 90);

        fusionSlam.addPose(pose1);
        fusionSlam.addPose(pose2);

        assertEquals(pose1, fusionSlam.getPoseAtTime(1), "Pose at time 1 should match.");
        assertEquals(pose2, fusionSlam.getPoseAtTime(2), "Pose at time 2 should match.");
    }

    @Test
    void testGlobalTransformation() {
        Pose pose = new Pose(1, 2, 2, 45);
        List<CloudPoint> localPoints = Arrays.asList(
                new CloudPoint(1, 1),
                new CloudPoint(2, 2));

        List<CloudPoint> globalPoints = fusionSlam.transformToGlobal(localPoints, pose);

        assertNotNull(globalPoints, "Global points should not be null.");
        assertEquals(2, globalPoints.size(), "Global points size should match local points size.");
    }

    @Test
    void testProcessTrackedObject_NewLandmark() {
        Pose pose = new Pose(1, 0, 0, 0);
        fusionSlam.addPose(pose);

        TrackedObject trackedObject = new TrackedObject("L1", 1, "Landmark", Arrays.asList(
                new CloudPoint(1, 1)));

        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject));

        List<LandMark> landmarks = fusionSlam.getLandmarks();
        assertEquals(1, landmarks.size(), "One landmark should be added.");
        assertEquals("L1", landmarks.get(0).getId(), "Landmark ID should match.");
    }

    @Test
    void testProcessTrackedObject_UpdateLandmark() {
        Pose pose = new Pose(1, 0, 0, 0);
        fusionSlam.addPose(pose);

        TrackedObject trackedObject1 = new TrackedObject("L1", 1, "Landmark", Arrays.asList(
                new CloudPoint(1, 1)));

        TrackedObject trackedObject2 = new TrackedObject("L1", 1, "Landmark", Arrays.asList(
                new CloudPoint(2, 2)));

        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject1));
        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject2));

        List<LandMark> landmarks = fusionSlam.getLandmarks();
        assertEquals(1, landmarks.size(), "Only one landmark should exist.");
        assertEquals("L1", landmarks.get(0).getId(), "Landmark ID should match.");
        assertEquals(1.5, landmarks.get(0).getCoordinates().get(0).getX(), 0.01, "X coordinate should be averaged.");
        assertEquals(1.5, landmarks.get(0).getCoordinates().get(0).getY(), 0.01, "Y coordinate should be averaged.");
    }

    @Test
    void testProcessTrackedObject_NoPose() {
        List<CloudPoint> coordinates = Arrays.asList(new CloudPoint(1, 1));
        TrackedObject trackedObject = new TrackedObject("L1", 2, "Landmark", coordinates);

        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject));

        List<LandMark> landmarks = fusionSlam.getLandmarks();
        assertTrue(landmarks.isEmpty(), "No landmarks should be added if pose is missing.");
    }

    @Test
    void testMultipleTrackedObjects() {
        Pose pose = new Pose(1, 0, 0, 0);
        fusionSlam.addPose(pose);

        TrackedObject trackedObject1 = new TrackedObject("L1", 1, "Landmark1", Arrays.asList(
                new CloudPoint(1, 1)));

        TrackedObject trackedObject2 = new TrackedObject("L2", 1, "Landmark2", Arrays.asList(
                new CloudPoint(2, 2)));

        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject1, trackedObject2));

        List<LandMark> landmarks = fusionSlam.getLandmarks();
        assertEquals(2, landmarks.size(), "Two landmarks should be added.");
    }

    @Test
    void testEmptyTrackedObjects() {
        fusionSlam.processTrackedObjects(Arrays.asList());
        assertTrue(fusionSlam.getLandmarks().isEmpty(), "No landmarks should be added for empty tracked objects.");
    }
}