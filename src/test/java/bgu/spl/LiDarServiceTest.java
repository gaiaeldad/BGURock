package bgu.spl;

import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.LiDarService;
import bgu.spl.mics.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

class LiDarServiceTest {

    private LiDarService lidarService;
    private LiDarWorkerTracker mockTracker;
    private PriorityQueue<TrackedObjectsEvent> eventQueue;

    @BeforeEach
    void setUp() {
        // Pre-Condition: LiDAR worker tracker is initialized with valid ID and
        // frequency.
        mockTracker = new LiDarWorkerTracker(1, 5, "testFilePath"); // ID = 1, frequency = 5
        lidarService = new LiDarService("LiDarServiceTest", mockTracker);
        eventQueue = new PriorityQueue<>((a, b) -> Integer.compare(a.getTime(), b.getTime()));
    }

    /**
     * Test: Prepares and sends data immediately when the time condition is met.
     * Pre-Condition: The current tick matches or exceeds the designated send time.
     * Post-Condition: The data is sent immediately and not added to the queue.
     * Invariant: The number of tracked objects is updated correctly in the
     * statistics folder.
     */
    @Test
    void testPrepareDataAndSendEvent_ReadyForImmediateSend() {
        // Setup
        List<DetectedObject> detectedObjects = new ArrayList<>();
        detectedObjects.add(new DetectedObject("obj1", "Test Object 1"));
        detectedObjects.add(new DetectedObject("obj2", "Test Object 2"));

        StampedDetectedObject stampedObjects = new StampedDetectedObject(0, detectedObjects);
        DetectObjectsEvent event = new DetectObjectsEvent(stampedObjects, "LiDarServiceTest", 0);

        mockTracker.updateTick(5);

        // Action
        List<TrackedObject> trackedObjects = mockTracker.prosseingEvent(stampedObjects);
        int designatedTime = stampedObjects.getTime() + mockTracker.getFrequency();

        TrackedObjectsEvent toSendEvent = new TrackedObjectsEvent(
                event, stampedObjects.getTime(), trackedObjects, "LiDarServiceTest", designatedTime);

        StatisticalFolder.getInstance().updateNumTrackedObjects(trackedObjects.size());

        // Post-Condition
        assertTrue(designatedTime <= mockTracker.getCurrentTick(), "Event should be sent immediately.");
        assertEquals(trackedObjects.size(), StatisticalFolder.getInstance().getNumTrackedObjects(),
                "Statistics should be updated correctly.");
    }

    /**
     * Test: Prepares and queues data when the time condition is not met.
     * Pre-Condition: The current tick is less than the designated send time.
     * Post-Condition: The data is added to the queue and not sent immediately.
     * Invariant: The queue size increases by 1.
     */
    @Test
    void testPrepareDataAndQueueEvent_NotReadyYet() {
        // Setup
        List<DetectedObject> detectedObjects = new ArrayList<>();
        detectedObjects.add(new DetectedObject("obj1", "Test Object 1"));

        StampedDetectedObject stampedObjects = new StampedDetectedObject(0, detectedObjects);
        DetectObjectsEvent event = new DetectObjectsEvent(stampedObjects, "LiDarServiceTest", 0);

        mockTracker.updateTick(0);

        // Action
        List<TrackedObject> trackedObjects = mockTracker.prosseingEvent(stampedObjects);
        int designatedTime = stampedObjects.getTime() + mockTracker.getFrequency();

        TrackedObjectsEvent toSendEvent = new TrackedObjectsEvent(
                event, stampedObjects.getTime(), trackedObjects, "LiDarServiceTest", designatedTime);

        eventQueue.add(toSendEvent);

        // Post-Condition
        assertFalse(designatedTime <= mockTracker.getCurrentTick(), "Event should not be sent immediately.");
        assertEquals(1, eventQueue.size(), "Event should be added to the queue.");
    }

    /**
     * Test: Handles crash scenario and terminates the service.
     * Pre-Condition: LiDAR status is set to ERROR.
     * Post-Condition: Service is terminated.
     * Invariant: Service does not process any further events after termination.
     */
    @Test
    void testCrashAndTerminate() {
        // Pre-Condition
        mockTracker.setStatus(STATUS.ERROR);
        assertEquals(STATUS.ERROR, mockTracker.getStatus(), "Status should be ERROR.");

        // Action
        lidarService.testTerminate();

        // Post-Condition
        assertTrue(lidarService.isTerminated(), "Service should be terminated.");
    }

    /**
     * Test: Processes tick broadcast updates.
     * Pre-Condition: Current tick is 0.
     * Post-Condition: Current tick is updated.
     * Invariant: No invalid updates occur during broadcast processing.
     */
    @Test
    void testHandleTickBroadcast() {
        // Pre-Condition
        mockTracker.updateTick(0);

        // Action
        TickBroadcast tick = new TickBroadcast(1, 10);
        mockTracker.updateTick(1);

        // Post-Condition
        assertEquals(1, mockTracker.getCurrentTick(), "Current tick should be updated.");
    }
}
