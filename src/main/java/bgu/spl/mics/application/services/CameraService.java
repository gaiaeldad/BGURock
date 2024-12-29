package bgu.spl.mics.application.services;

import java.util.List;
import bgu.spl.mics.Broadcast;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.StampedDetectedObject;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {
    private final Camera camera;

    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera) {
        super("CameraService" + camera.getId());
        this.camera = camera;
    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts and sets up callbacks for sending
     * DetectObjectsEvents.
     */
    @Override
    protected void initialize() {
        // Subscribe to TickBroadcast
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast broadcast) -> {
            int currentTime = broadcast.getTime();

            // Check if the camera is active and it's time to send an event
            //--------------------לוודא את עניין הזמנים שוב
            if (camera.getStatus() == STATUS.UP) {
                StampedDetectedObject detectedObject = camera.getDetectedObjectsAtTime(currentTime + camera.getFrequency());
                if (camera.getStatus() == STATUS.ERROR){
                    terminate();
                    sendBroadcast(new CrashedBroadcast(camera.getErrMString(), this.getName()));
                }
                else{
                  if (detectedObject != null) {
                    DetectObjectsEvent event = new DetectObjectsEvent(detectedObject, getName());
                    sendEvent(event);
                    StatisticalFolder.getInstance().updateNumDetectedObjects(detectedObject.getDetectedObjects().size());
                  }   
                }
            }
            else {//camers is down 
                terminate();
                sendBroadcast(new TerminatedBroadcast(getName()));     
            }
        });
//--------------------------------------זה לא נכון צריך לתקן ולהבין מה לעשות עם ההרשמות האלו ------------------------------------------------------------
        // Subscribe to TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast broadcast) -> {
            // what should we do here?
        });
        // Subscribe to TerminatedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast broadcast) -> {
            terminate();
            sendBroadcast(new TerminatedBroadcast(getName()));   
        });
        
    }
}