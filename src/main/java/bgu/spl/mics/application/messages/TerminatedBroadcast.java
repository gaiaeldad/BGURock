package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

/**
 * A broadcast that represents the termination of the program.
 */
// TerminatedBroadcast class
public class TerminatedBroadcast implements Broadcast {
    private String senderName;

    public TerminatedBroadcast(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderName() {
        return senderName;
    }
}