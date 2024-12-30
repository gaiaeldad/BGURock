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

//import static org.junit.Assert.assertSame;

//import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 * 
 * This service interacts with the LiDarWorkerTracker object to retrieve and process
 * cloud point data and updates the system's StatisticalFolder upon sending its
 * observations.
 */
public class LiDarService extends MicroService {

    private final LiDarWorkerTracker lidarWorkerTracker;
    private  PriorityQueue<TrackedObjectsEvent> eventQueue;


    public LiDarService(String name, LiDarWorkerTracker lidarWorkerTracker) {
        super(name);
        this.lidarWorkerTracker = lidarWorkerTracker;
        this.eventQueue = new PriorityQueue<>(Comparator.comparingInt(event -> event.getTime()));

    }

    @Override
    protected void initialize() {
        subscribeBroadcast(TickBroadcast.class, tick -> {
            int currentTime = tick.getTime();
            lidarWorkerTracker.updateTick(currentTime);
            while (!eventQueue.isEmpty()) {
                TrackedObjectsEvent event = eventQueue.peek();
                if (event.getdesignatedTime() > currentTime) {
                    break;
                }
                TrackedObjectsEvent readyEvent = eventQueue.poll();
                complete(readyEvent.getHandeledEvent(), true);
                sendEvent(readyEvent);
                StatisticalFolder.getInstance().updateNumDetectedObjects(
                    readyEvent.getTrackedObjects().size());
            }  
        });

        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast broadcast) -> {
        //----------------------fill- what should we do here 
        });
        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast broadcast) -> {
            terminate();
            sendBroadcast(new TerminatedBroadcast(getName()));
        //should we do more?
        });
    
        //--------------------לוודא את עניין הזמנים שוב
        subscribeEvent(DetectObjectsEvent.class, event -> {
            if(lidarWorkerTracker.getStatus()==STATUS.UP){   
                List<TrackedObject> TrackedObjects = lidarWorkerTracker.prosseingEvent(event.getStampedDetectedObjects());
               if (lidarWorkerTracker.getStatus()==STATUS.ERROR){
                    terminate();
                    sendBroadcast(new CrashedBroadcast("LidarWorker" + lidarWorkerTracker.getId() + "disconnected", this.getName()));
               }
               else{
                    int designatedTime = event.getStampedDetectedObjects().getTime() + lidarWorkerTracker.getFrequency();
                    int currTime = lidarWorkerTracker.getCurrentTick();
                    TrackedObjectsEvent toSendEvent = (new TrackedObjectsEvent(event, event.getStampedDetectedObjects().getTime(), TrackedObjects, getName(), designatedTime));
                    if (designatedTime <= currTime){ /////----------לבדוק תנאי ראשון
                        complete(event, true);
                        sendEvent(toSendEvent);
                        StatisticalFolder.getInstance().updateNumTrackedObjects(TrackedObjects.size());
                    }
                    else{
                        eventQueue.add(toSendEvent);
                    }
               }
               if (lidarWorkerTracker.getStatus() == STATUS.DOWN){//מתי זה הופך ללמטה???
                    terminate();
                    sendBroadcast(new TerminatedBroadcast(getName()));  
                }
            }
            else{//down
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));    
            }          
        });


    }
        
}