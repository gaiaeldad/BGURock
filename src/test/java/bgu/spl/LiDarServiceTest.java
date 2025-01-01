package bgu.spl;

import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.*;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.services.LiDarService;

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
        mockTracker = new LiDarWorkerTracker(1, 5, "testFilePath"); // ID = 1, frequency = 5
        lidarService = new LiDarService("LiDarServiceTest", mockTracker);
        eventQueue = new PriorityQueue<>(
                (a, b) -> Integer.compare(a.getTime(), b.getTime()));
    }

    @Test // checking for events that a ready to send now and dont need to go to the queue
    void testPrepareDataAndSendEvent_ReadyForImmediateSend() {
        List<DetectedObject> detectedObjects = new ArrayList<>();
        detectedObjects.add(new DetectedObject("obj1", "Test Object 1"));
        detectedObjects.add(new DetectedObject("obj2", "Test Object 2"));

        StampedDetectedObject stampedObjects = new StampedDetectedObject(0, detectedObjects);
        DetectObjectsEvent event = new DetectObjectsEvent(stampedObjects, "LiDarServiceTest", 0);

        mockTracker.updateTick(5);

        // it will be able to send if the current tick matches or is more than the
        // designatedTime to send
        List<TrackedObject> trackedObjects = mockTracker.prosseingEvent(stampedObjects);
        int designatedTime = stampedObjects.getTime() + mockTracker.getFrequency();

        TrackedObjectsEvent toSendEvent = new TrackedObjectsEvent(
                event, stampedObjects.getTime(), trackedObjects, "LiDarServiceTest", designatedTime);

        StatisticalFolder.getInstance().updateNumTrackedObjects(trackedObjects.size());

        assertTrue(designatedTime <= mockTracker.getCurrentTick());
        assertEquals(trackedObjects.size(), StatisticalFolder.getInstance().getNumTrackedObjects());
    }

    @Test // checking for events that are not ready to send now and need to go to the
          // queue
    void testPrepareDataAndQueueEvent_NotReadyYet() {
        List<DetectedObject> detectedObjects = new ArrayList<>();
        detectedObjects.add(new DetectedObject("obj1", "Test Object 1"));

        StampedDetectedObject stampedObjects = new StampedDetectedObject(0, detectedObjects);
        DetectObjectsEvent event = new DetectObjectsEvent(stampedObjects, "LiDarServiceTest", 0);

        mockTracker.updateTick(0);

        List<TrackedObject> trackedObjects = mockTracker.prosseingEvent(stampedObjects);
        int designatedTime = stampedObjects.getTime() + mockTracker.getFrequency();

        TrackedObjectsEvent toSendEvent = new TrackedObjectsEvent(
                event, stampedObjects.getTime(), trackedObjects, "LiDarServiceTest", designatedTime);

        assertFalse(designatedTime <= mockTracker.getCurrentTick());
        eventQueue.add(toSendEvent);
        assertEquals(1, eventQueue.size());
    }

    @Test
    void testCrashAndTerminate() {
        mockTracker.setStatus(STATUS.ERROR);
        assertEquals(STATUS.ERROR, mockTracker.getStatus());

        lidarService.testTerminate();
        assertTrue(lidarService.isTerminatedForTest());

    }

    @Test
    void testHandleTickBroadcast() {
        TickBroadcast tick = new TickBroadcast(1, 10);
        mockTracker.updateTick(1);

        assertEquals(1, mockTracker.getCurrentTick());
    }
}