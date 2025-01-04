package bgu.spl.mics;

import java.util.*;
import java.util.concurrent.*;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus
 * interface.
 * Write your implementation here!
 * Only private fields and methods can be added to this class.
 */
public class MessageBusImpl implements MessageBus {
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
    public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
        eventSubscribers.putIfAbsent(type, new ConcurrentLinkedQueue<>());
        Queue<MicroService> subscribers = eventSubscribers.get(type);
        synchronized (subscribers) {
            if (!subscribers.contains(m)) { // Ensure the microservice is not registered twice
                subscribers.add(m);
            }
        }
    }

    @Override
    public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
        broadcastSubscribers.putIfAbsent(type, new CopyOnWriteArrayList<>());
        List<MicroService> subscribers = broadcastSubscribers.get(type);
        synchronized (subscribers) {
            if (!subscribers.contains(m)) { // Ensure the microservice is not registered twice
                subscribers.add(m);
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
            if (subscribers != null && !subscribers.isEmpty()) {
                for (MicroService m : subscribers) {
                    try {
                        // Retrieve the queue of the microservice
                        BlockingQueue<Message> queue = microServiceQueues.get(m);
                        // Check if the queue exists
                        if (queue == null) {
                            throw new IllegalStateException("Queue for the MicroService does not exist.");
                        }
                        // Add the message to the queue
                        queue.put(b);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restore the interrupt status
                        e.printStackTrace(); // Print the stack trace for debugging
                    } catch (IllegalStateException ex) {
                        System.err.println("Error: " + ex.getMessage()); // Print the error message
                    }
                }
            } else {
                System.out.println("No subscribers found for the broadcast: " + b.getClass().getName());
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
            return null; // Return null instead of trying to access a null subscriber
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
            ex.printStackTrace(); // Handle InterruptedException
        }

        // Return the Future of the event
        return future;
    }

    @Override
    public void register(MicroService m) {
        microServiceQueues.putIfAbsent(m, new LinkedBlockingQueue<>());
    }

    @Override
    public void unregister(MicroService m) {
        microServiceQueues.remove(m);
        for (Queue<MicroService> subscribers : eventSubscribers.values()) {
            synchronized (subscribers) {
                subscribers.remove(m);
            }
        }
        for (List<MicroService> subscribers : broadcastSubscribers.values()) {
            subscribers.remove(m);
        }
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

}