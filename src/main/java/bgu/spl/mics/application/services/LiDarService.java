package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.TrackedObject;
import bgu.spl.mics.application.objects.StatisticalFolder;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 * 
 * This service interacts with the LiDarWorkerTracker object to retrieve and
 * process
 * cloud point data and updates the system's StatisticalFolder upon sending its
 * observations.
 */
// LiDarService class

public class LiDarService extends MicroService {

    private final LiDarWorkerTracker lidarWorkerTracker;
    private PriorityQueue<TrackedObjectsEvent> TOeventQueue;

    public LiDarService(String name, LiDarWorkerTracker lidarWorkerTracker) {
        super(name);
        this.lidarWorkerTracker = lidarWorkerTracker;
        this.TOeventQueue = new PriorityQueue<>(Comparator.comparingInt(event -> event.getTime()));

    }

    @Override
    protected void initialize() {
        subscribeBroadcast(TickBroadcast.class, tick -> {
            System.out.println(getName() + ": recived tickBrodcast, tick: " + tick.getTime());
            int currentTime = tick.getTime();
            if (lidarWorkerTracker.getStatus() != STATUS.UP) {
                return;
            }
            lidarWorkerTracker.updateTick(currentTime);
            while (!TOeventQueue.isEmpty()) {
                TrackedObjectsEvent event = TOeventQueue.peek();
                if (event.getDesignatedTime() > currentTime) {
                    break;
                }
                TrackedObjectsEvent readyEvent = TOeventQueue.poll();
                complete(readyEvent.getHandeledEvent(), true);
                lidarWorkerTracker.setLastTrackedObjects(readyEvent.getTrackedObjects());// update the last tracked
                                                                                         // objects
                System.out.println(getName() + ": sent TrackedObjectsEvent at time" + currentTime
                        + "for object from time" + event.getTime());
                System.out.println(getName() + ": sent an event");
                sendEvent(readyEvent);
                StatisticalFolder.getInstance().updateNumDetectedObjects(readyEvent.getTrackedObjects().size());

            }
            if (TOeventQueue.isEmpty() && (lidarWorkerTracker.getStatus() == STATUS.DOWN)) {
                System.out.println(getName() + ": is down, finished and terminated");
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));
            }

        });

        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast broadcast) -> {
            if ("TimeService".equals(broadcast.getSenderName())) {
                System.out.println(getName() + "recived TerminatedBroadcast from TimeService");
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));
            }
        });
        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast broadcast) -> {
            System.out.println(getName() + "recived CrashedBroadcast from" + broadcast.getSenderName());
            terminate();
        });

        subscribeEvent(DetectObjectsEvent.class, event -> {
            System.out.println(getName() + ": recived DetectObjectsEvent from: " + event.getSenderName() + "for time:"
                    + event.getStampedDetectedObjects().getTime());
            if (lidarWorkerTracker.getStatus() == STATUS.UP) {
                List<TrackedObject> TrackedObjects = lidarWorkerTracker
                        .prosseingEvent(event.getStampedDetectedObjects());
                if (lidarWorkerTracker.getStatus() == STATUS.ERROR) {
                    System.out.println(getName() + " eror");
                    terminate();
                    sendBroadcast(new CrashedBroadcast("LidarWorker" + lidarWorkerTracker.getId() + "disconnected",
                            this.getName() + "" + "LidarWorker" + lidarWorkerTracker.getId()));
                } else {
                    int designatedTime = event.getStampedDetectedObjects().getTime()
                            + lidarWorkerTracker.getFrequency();
                    int currTime = lidarWorkerTracker.getCurrentTick();
                    TrackedObjectsEvent toSendEvent = (new TrackedObjectsEvent(event,
                            event.getStampedDetectedObjects().getTime(), TrackedObjects, getName(), designatedTime));
                    if (designatedTime <= currTime) { ///// ----------לבדוק תנאי ראשון
                        complete(event, true);
                        System.out.println(getName() + ": sent TrackedObjectsEvent at: " + currTime
                                + "for object from time: " + event.getStampedDetectedObjects().getTime());
                        sendEvent(toSendEvent);
                        lidarWorkerTracker.setLastTrackedObjects(TrackedObjects);// update the last tracked objects
                        StatisticalFolder.getInstance().updateNumTrackedObjects(TrackedObjects.size());
                        StatisticalFolder.getInstance().updateLastFrame(getName(), toSendEvent);
                    } else {
                        TOeventQueue.add(toSendEvent);
                    }
                }
                if (TOeventQueue.isEmpty() && lidarWorkerTracker.getStatus() == STATUS.DOWN) {
                    System.out.println(getName() + ": is down, finished and terminated");
                    terminate();
                    sendBroadcast(new TerminatedBroadcast(getName()));
                }
            } else if (TOeventQueue.isEmpty()) {
                System.out.println(getName() + ": is down, finished and terminated");
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));
            }
        });

    }

    // functions for testing
    // Terminates the service for testing purposes
    public void testTerminate() {
        terminate(); // Calls the inherited terminate method to stop the service.
    }

}