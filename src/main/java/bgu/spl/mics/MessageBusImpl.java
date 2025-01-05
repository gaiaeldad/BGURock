package bgu.spl.mics;

import java.util.concurrent.*;
import java.util.*;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus
 * interface.
 * Write your implementation here!
 * Only private fields and methods can be added to this class.
 */
// MessageBusImpl class
// our implemetion of message bus
public class MessageBusImpl implements MessageBus {
    // fileds
    private final Map<Class<? extends Event<?>>, Queue<MicroService>> eventSubscribers = new ConcurrentHashMap<>();
    private final Map<Class<? extends Broadcast>, List<MicroService>> broadcastSubscribers = new ConcurrentHashMap<>();
    private final Map<Event<?>, Future<?>> eventFutures = new ConcurrentHashMap<>();
    private final Map<MicroService, BlockingQueue<Message>> microServiceQueues = new ConcurrentHashMap<>();

    private static class SingletonHolderMessageBusImpl { // Implementation as shown in class
        private static final MessageBusImpl INSTANCE = new MessageBusImpl();
    }

    public static MessageBusImpl getInstance() {
        return SingletonHolderMessageBusImpl.INSTANCE;
    }

    @Override
    public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
        broadcastSubscribers.putIfAbsent(type, new CopyOnWriteArrayList<>());
        List<MicroService> subscribers = broadcastSubscribers.get(type);
        synchronized (subscribers) {
            if (!subscribers.contains(m)) { // Ensure the microservice is not registered twice
                subscribers.add(m);
                System.out.println(m.getName() + " subscribed to Broadcast: " + type.getSimpleName());
            }
        }
    }

    @Override
    public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
        eventSubscribers.putIfAbsent(type, new ConcurrentLinkedQueue<>());
        Queue<MicroService> subscribers = eventSubscribers.get(type);
        synchronized (subscribers) {
            if (!subscribers.contains(m)) { // Ensure the microservice is not registered twice
                subscribers.add(m);
                System.out.println(m.getName() + " subscribed to event: " + type.getSimpleName());
            }
        }
    }

    /**
     * Updates the Future of the event when it receives the result of execution.
     * Uses the sent data to resolve the corresponding Future.
     */
    @Override
    public <T> void complete(Event<T> e, T result) {
        @SuppressWarnings("unchecked")
        Future<T> future = (Future<T>) eventFutures.get(e);
        if (future != null) {
            future.resolve(result); // Update the result
            eventFutures.remove(e);
        }
    }

    @Override
    public void sendBroadcast(Broadcast b) {
        // Retrieve the list of subscribers for this broadcast type
        List<MicroService> subscribers = broadcastSubscribers.get(b.getClass());
        synchronized (subscribers) {
            // Check if there are any subscribers
            if (subscribers == null || subscribers.isEmpty()) {
                System.out.println("No subscribers found for broadcast: " + b.getClass().getSimpleName());
            } else {
                for (MicroService m : subscribers) {
                    // Check if the microservice is registered
                    if (!microServiceQueues.containsKey(m)) {
                        System.out.println(
                                "Error: MicroService " + m.getName() + " is not registered in microServiceQueues.");
                    }
                    try {
                        // Add the broadcast to the microservice's queue
                        microServiceQueues.get(m).put(b);
                    } catch (InterruptedException e) {
                        // Handle InterruptedException
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /**
     * Sends an event to a subscribed microservice (if there is a subscriber).
     * If there are subscribers, the event is sent according to the round-robin
     * principle (default: the first microservice).
     */
    @Override
    public <T> Future<T> sendEvent(Event<T> e) {
        // Check if there are subscribers for this event type
        Queue<MicroService> subscribers = eventSubscribers.get(e.getClass());
        if (subscribers == null || subscribers.isEmpty()) { // If there are no subscribers
            return null;
        }

        MicroService selectedService;
        synchronized (subscribers) {
            // Select a microservice to send the event to (here we chose the first in the
            // list)
            selectedService = subscribers.poll(); // chose the first microservice
            if (selectedService != null) {
                subscribers.add(selectedService); // Move it to the end of the queue
            }
            if (selectedService == null || !microServiceQueues.containsKey(selectedService)) {
                return null; // No valid service to handle the event
            }
        }
        Future<T> future = new Future<>();
        // Save the Future of the event so we can return the result later
        eventFutures.putIfAbsent(e, future);
        try {
            microServiceQueues.get(selectedService).put(e); // Add the event to the queue
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); // Restore the interrupt status
        }

        // Return the Future of the event
        return future;
    }

    @Override
    public Message awaitMessage(MicroService m) throws InterruptedException {
        if (!microServiceQueues.containsKey(m)) {
            throw new IllegalStateException("MicroService is not registered with the MessageBus.");
        }
        BlockingQueue<Message> queue = microServiceQueues.get(m);
        if (queue == null) {
            throw new IllegalStateException("MicroService " + m.getName() + " is not registered.");
        }
        return queue.take();
    }

    public Map<MicroService, BlockingQueue<Message>> getMicroServiceQueues() {
        return microServiceQueues;
    }

    @Override
    public void register(MicroService m) {
        microServiceQueues.putIfAbsent(m, new LinkedBlockingQueue<>());
        System.out.println("Registered MicroService: " + m.getName());
    }

    @Override
    public void unregister(MicroService m) {
        if (microServiceQueues.containsKey(m)) {
            microServiceQueues.remove(m);
            for (Queue<MicroService> subscribers : eventSubscribers.values()) {
                synchronized (subscribers) {
                    subscribers.remove(m);
                }
            }
            for (List<MicroService> subscribers : broadcastSubscribers.values()) {
                synchronized (subscribers) {
                    subscribers.remove(m);
                }
            }
            System.out.println("Unregistered MicroService: " + m.getName());
        }
    }
    // functions for testing

    // Checks if the given microservice is registered in the MessageBus
    public boolean isRegistered(MicroService m) {
        return microServiceQueues.containsKey(m);
    }

    // Returns the number of currently registered microservices
    public int getNumberOfRegisters() {
        return microServiceQueues.size();
    }

    // Checks if the given microservice is subscribed to a specific Broadcast type
    public boolean isSubscribedToBroad(Class<? extends Broadcast> type, MicroService m) {
        List<MicroService> subscribers = broadcastSubscribers.get(type);
        return subscribers != null && subscribers.contains(m);
    }

    // Returns the number of subscribers to a specific Broadcast type
    public int getNumberOfSubscribersToBroad(Class<? extends Broadcast> type) {
        List<MicroService> subscribers = broadcastSubscribers.get(type);
        return subscribers == null ? 0 : subscribers.size();
    }

    // Checks if the given microservice is subscribed to a specific Event type
    public boolean isSubscribedToEvent(Class<? extends Event<?>> type, MicroService m) {
        Queue<MicroService> subscribers = eventSubscribers.get(type);
        return subscribers != null && subscribers.contains(m);
    }

    // Returns the number of subscribers to a specific Event type
    public int getNumberOfSubscribersToEvent(Class<? extends Event<?>> type) {
        Queue<MicroService> subscribers = eventSubscribers.get(type);
        return subscribers == null ? 0 : subscribers.size();
    }

}
