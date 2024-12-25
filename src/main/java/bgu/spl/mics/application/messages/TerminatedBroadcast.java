package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class TerminatedBroadcast implements Broadcast{    
    private final String serviceName;
    private String senderId;


    public TerminatedBroadcast(String serviceName, String senderId) {
        this.serviceName = serviceName;
        this.senderId = senderId;
    }
    public String getServiceName() {
        return serviceName;
    }
    public String getSenderId() {
        return senderId;
    }
}