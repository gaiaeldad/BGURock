package bgu.spl.mics.application.messages;
import bgu.spl.mics.Broadcast;

public class CrashedBroadcast implements Broadcast {
    private final String errorMessage;
    private String senderName;

    public CrashedBroadcast(String errorMessage,String senderName) {
        this.errorMessage = errorMessage;
        this.senderName = senderName;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    public String getSenderId() {
        return senderName;
    }

}