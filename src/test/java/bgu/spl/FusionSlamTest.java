package bgu.spl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import bgu.spl.mics.application.objects.CloudPoint;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.LandMark;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FusionSlamTest {

    private FusionSlam fusionSlam;

    @BeforeEach
    void setup() {
        fusionSlam = FusionSlam.getInstance();
        fusionSlam.clearLandmarks();
    }

    @Test
    void testGlobalTransformation() {
        Pose pose = new Pose(1, 1.0f, 1.0f, 45.0f);
        List<CloudPoint> localCoordinates = Arrays.asList(
                new CloudPoint(1.0, 1.0),
                new CloudPoint(2.0, 2.0),
                new CloudPoint(0.0, 1.0));

        List<CloudPoint> globalCoordinates = fusionSlam.transformToGlobal(localCoordinates, pose);

        // Ensure the transformation was successful
        assertNotNull(globalCoordinates, "The transformation result should not be null.");
        assertEquals(3, globalCoordinates.size(), "The number of global points should match the local points.");

        // Validate the transformed points against expected values
        double sqrt2 = Math.sqrt(2) / 2;
        CloudPoint expectedPoint1 = new CloudPoint(1.0 + sqrt2 - sqrt2, 1.0 + sqrt2 + sqrt2);
        CloudPoint expectedPoint2 = new CloudPoint(1.0 + 2 * sqrt2 - 2 * sqrt2, 1.0 + 2 * sqrt2 + 2 * sqrt2);
        CloudPoint expectedPoint3 = new CloudPoint(1.0 + 0 * sqrt2 - 1 * sqrt2, 1.0 + 0 * sqrt2 + 1 * sqrt2);

        assertEquals(expectedPoint1.getX(), globalCoordinates.get(0).getX(), 0.0001,
                "First point X-coordinate mismatch");
        assertEquals(expectedPoint1.getY(), globalCoordinates.get(0).getY(), 0.0001,
                "First point Y-coordinate mismatch");
        assertEquals(expectedPoint2.getX(), globalCoordinates.get(1).getX(), 0.0001,
                "Second point X-coordinate mismatch");
        assertEquals(expectedPoint2.getY(), globalCoordinates.get(1).getY(), 0.0001,
                "Second point Y-coordinate mismatch");
        assertEquals(expectedPoint3.getX(), globalCoordinates.get(2).getX(), 0.0001,
                "Third point X-coordinate mismatch");
        assertEquals(expectedPoint3.getY(), globalCoordinates.get(2).getY(), 0.0001,
                "Third point Y-coordinate mismatch");
    }

    @Test
    void testAddNewLandmark() {
        Pose pose = new Pose(1, 1.0f, 0.0f, 1.0f);
        fusionSlam.addPose(pose);

        List<CloudPoint> coordinates = Arrays.asList(
                new CloudPoint(1.0, 1.0),
                new CloudPoint(2.0, 2.0));

        TrackedObject trackedObject = new TrackedObject("Landmark1", 1, "Newly added landmark", coordinates);
        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject));

        // Verify the landmark was added correctly
        List<LandMark> landmarks = fusionSlam.getLandmarksMod();
        assertEquals(1, landmarks.size(), "Exactly one new landmark should have been added.");
        assertEquals("Landmark1", landmarks.get(0).getId(),
                "The ID of the new landmark should match the expected value.");
        assertEquals(2, landmarks.get(0).getCoordinates().size(),
                "The number of coordinates for the new landmark should be correct.");
    }

    @Test
    void testUpdateExistingLandmark() {
        Pose pose = new Pose(1, 1.0f, 0.0f, 1.0f);
        fusionSlam.addPose(pose);

        List<CloudPoint> initialCoordinates = Arrays.asList(
                new CloudPoint(1.0, 1.0),
                new CloudPoint(2.0, 2.0));

        LandMark existingLandmark = new LandMark("Landmark1", "Test landmark", initialCoordinates);
        fusionSlam.addLandmark(existingLandmark);

        List<CloudPoint> newCoordinates = Arrays.asList(
                new CloudPoint(3.0, 3.0),
                new CloudPoint(4.0, 4.0));

        TrackedObject trackedObject = new TrackedObject("Landmark1", 1, "Updated landmark", newCoordinates);
        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject));

        // Ensure the existing landmark was updated
        List<LandMark> landmarks = fusionSlam.getLandmarksMod();
        assertEquals(1, landmarks.size(), "The total number of landmarks should remain the same.");
        assertEquals("Landmark1", landmarks.get(0).getId(),
                "The ID of the updated landmark should match the original.");
        assertEquals(2, landmarks.get(0).getCoordinates().size(),
                "The updated landmark should retain the correct number of coordinates.");
    }

    @Test
    void testProcessTrackedObjectsWithPose() {
        Pose pose = new Pose(2, 1.0f, 0.0f, 0.0f);
        fusionSlam.addPose(pose);

        List<CloudPoint> coordinates = Arrays.asList(
                new CloudPoint(1.0, 1.0),
                new CloudPoint(2.0, 2.0));

        TrackedObject trackedObject = new TrackedObject("Landmark1", 2, "Tracked object landmark", coordinates);
        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject));

        // Verify the object was added as a landmark
        List<LandMark> landmarks = fusionSlam.getLandmarksMod();
        assertEquals(1, landmarks.size(), "One landmark should be added to the system.");
        assertEquals("Landmark1", landmarks.get(0).getId(), "The added landmark should have the correct ID.");
        assertEquals(2, landmarks.get(0).getCoordinates().size(),
                "The added landmark should have the correct number of coordinates.");
    }

    @Test
    void testProcessTrackedObjectsWithoutPose() {
        fusionSlam.clearLandmarks();

        List<CloudPoint> coordinates = Arrays.asList(
                new CloudPoint(1.0, 1.0),
                new CloudPoint(2.0, 2.0));

        TrackedObject trackedObject = new TrackedObject("Landmark1", 3, "Unassociated tracked object", coordinates);
        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject));

        // Ensure no landmarks are added without a pose
        List<LandMark> landmarks = fusionSlam.getLandmarksMod();
        assertEquals(0, landmarks.size(), "No landmark should be added if no pose exists.");
    }

    @Test
    void testProcessTrackedObjectsWithMultipleObjects() {
        Pose pose = new Pose(1, 1.0f, 45.0f, 1.0f);
        fusionSlam.addPose(pose);

        List<CloudPoint> coordinates1 = Arrays.asList(
                new CloudPoint(1.0, 1.0),
                new CloudPoint(2.0, 2.0));

        List<CloudPoint> coordinates2 = Arrays.asList(
                new CloudPoint(3.0, 3.0),
                new CloudPoint(4.0, 4.0));

        TrackedObject trackedObject1 = new TrackedObject("Landmark1", 1, "First object", coordinates1);
        TrackedObject trackedObject2 = new TrackedObject("Landmark2", 1, "Second object", coordinates2);

        fusionSlam.processTrackedObjects(Arrays.asList(trackedObject1, trackedObject2));

        // Verify that multiple landmarks were added correctly
        List<LandMark> landmarks = fusionSlam.getLandmarksMod();
        assertEquals(2, landmarks.size(), "Two landmarks should have been added.");
        assertEquals("Landmark1", landmarks.get(0).getId(), "The ID of the first landmark should match.");
        assertEquals("Landmark2", landmarks.get(1).getId(), "The ID of the second landmark should match.");
    }
}
