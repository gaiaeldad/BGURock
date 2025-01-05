package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

// CrashedBroadcast class
public class CrashedBroadcast implements Broadcast {
    private final String errorMessage;
    private String senderName;

    public CrashedBroadcast(String errorMessage, String senderName) {
        this.errorMessage = errorMessage;
        this.senderName = senderName;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}