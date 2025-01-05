
package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class TerminateSensor implements Broadcast {
    private String senderName;

    public TerminateSensor(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderName() {
        return senderName;
    }
}