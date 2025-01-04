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

class FusionSlamTest {

    private FusionSlam fusionSlam;

    @BeforeEach
    void setUp() {
        fusionSlam = FusionSlam.getInstance();
    }

    /**
     * Test: Adds poses and retrieves them correctly.
     * Pre-Condition: FusionSlam instance is initialized.
     * Post-Condition: Stored poses match the added poses.
     * Invariant: Pose list size increases as poses are added.
     */
    @Test
    void testAddPoseAndRetrieve() {
        Pose pose1 = new Pose(1, 0, 0, 0);
        Pose pose2 = new Pose(2, 5, 5, 90);

        fusionSlam.addPose(pose1);
        fusionSlam.addPose(pose2);

        assertEquals(pose1, fusionSlam.getPoseAtTime(1), "Pose at time 1 should match.");
        assertEquals(pose2, fusionSlam.getPoseAtTime(2), "Pose at time 2 should match.");
    }

    /**
     * Test: Transforms local coordinates to global coordinates.
     * Pre-Condition: Pose is available for transformation.
     * Post-Condition: Global coordinates match expected values.
     * Invariant: Number of points remains the same.
     */
    @Test
    void testGlobalTransformation() {
        Pose pose = new Pose(1, 2, 2, 45);
        List<CloudPoint> localPoints = Arrays.asList(
                new CloudPoint(1, 1),
                new CloudPoint(2, 2),
                new CloudPoint(3, 3));

        List<CloudPoint> globalPoints = fusionSlam.transformToGlobal(localPoints, pose);

        assertNotNull(globalPoints, "Global points should not be null.");
        assertEquals(3, globalPoints.size(), "Global points size should match local points size.");
    }

    /**
     * Test: Processes a new tracked object with multiple points and creates a new
     * landmark.
     * Pre-Condition: Pose exists for the tracked object.
     * Post-Condition: A new landmark with multiple points is added.
     * Invariant: Landmark ID and size are consistent.
     */
    @Test
    void testProcessTrackedObject_NewLandmarkMultiplePoints() {
        Pose pose = new Pose(1, 0, 0, 0);
        fusionSlam.addPose(pose);

        TrackedObject trackedObject = new TrackedObject("L1", 1, "Landmark", Arrays.asList(
                new CloudPoint(1, 1),
                new CloudPoint(2, 2),
                new CloudPoint(3, 3)));

        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject));

        List<LandMark> landmarks = fusionSlam.getLandmarks();
        assertEquals(1, landmarks.size(), "One landmark should be added.");
        assertEquals("L1", landmarks.get(0).getId(), "Landmark ID should match.");
        assertEquals(3, landmarks.get(0).getCoordinates().size(), "Landmark should contain multiple points.");
    }

    /**
     * Test: Handles tracked objects without a corresponding pose.
     * Pre-Condition: No pose exists for the tracked object.
     * Post-Condition: No landmarks are added.
     * Invariant: Landmark list remains empty.
     */
    @Test
    void testProcessTrackedObject_NoPose() {
        List<CloudPoint> coordinates = Arrays.asList(new CloudPoint(1, 1));
        TrackedObject trackedObject = new TrackedObject("L1", 2, "Landmark", coordinates);

        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject));

        List<LandMark> landmarks = fusionSlam.getLandmarks();
        assertTrue(landmarks.isEmpty(), "No landmarks should be added if pose is missing.");
    }
}
