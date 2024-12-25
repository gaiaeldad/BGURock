package bgu.spl.mics.application.messages;
import bgu.spl.mics.Broadcast;

public class CrashedBroadcast implements Broadcast {
    private final String errorMessage;
    private String senderId;

    public CrashedBroadcast(String errorMessage,String senderId) {
        this.errorMessage = errorMessage;
        this.senderId = senderId;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    public String getSenderId() {
        return senderId;
    }

}