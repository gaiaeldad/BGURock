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

import java.util.List;

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
    

    public LiDarService(String name, LiDarWorkerTracker lidarWorkerTracker) {
        super(name);
        this.lidarWorkerTracker = lidarWorkerTracker;
    }

    @Override
    protected void initialize() {
        subscribeBroadcast(TickBroadcast.class, tick -> {
            lidarWorkerTracker.updateTick(tick.getTime());//לבדוק זמנים
        //----------------------זונות של הצאט 2
        });
        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast broadcast) -> {
        //----------------------fill
        });
        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast broadcast) -> {
        //----------------------fill
        });
    
        //--------------------לוודא את עניין הזמנים שוב
        subscribeEvent(DetectObjectsEvent.class, (DetectObjectsEvent event) -> {
            //-------------------מתי הופכים אותו לDOWN?
            if(lidarWorkerTracker.getStatus()==STATUS.UP){   
                List<TrackedObject> TrackedObjects = lidarWorkerTracker.prosseingEvent(event.getStampedDetectedObjects());
                complete(event, true);//האירוע טופל
                //--------------להבין את הזמנים בדיוק
                sendEvent(new TrackedObjectsEvent(event.getStampedDetectedObjects().getTime(), TrackedObjects, getName()));
                StatisticalFolder.getInstance().updateNumTrackedObjects(TrackedObjects.size());
            }else{
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));    
            }
        });

    }
        
}