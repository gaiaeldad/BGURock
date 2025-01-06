package bgu.spl;

import bgu.spl.mics.*;
import bgu.spl.mics.example.messages.ExampleBroadcast;
import bgu.spl.mics.example.messages.ExampleEvent;
import bgu.spl.mics.example.services.ExampleBroadcastListenerService;
import bgu.spl.mics.example.services.ExampleEventHandlerService;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MessageBusImplTest {

        @Test
        void testRegisterEventSubscription() {
                MessageBusImpl messageBus = MessageBusImpl.getInstance();
                MicroService handler = new ExampleEventHandlerService("HandlerService", new String[] { "5" });

                // Register and verify
                messageBus.register(handler);
                assertTrue(messageBus.isRegistered(handler), "Handler should be registered with the message bus.");

                // Test duplicate registration
                messageBus.register(handler);
                assertEquals(1, messageBus.getNumberOfRegisters(), "Handler should only be registered once.");

                // Subscribe and verify event subscription
                messageBus.subscribeEvent(ExampleEvent.class, handler);
                assertTrue(messageBus.isSubscribedToEvent(ExampleEvent.class, handler),
                                "Handler should be subscribed to ExampleEvent.");

                // Cleanup
                messageBus.unregister(handler);
                assertFalse(messageBus.isRegistered(handler), "Handler should no longer be registered.");
        }

        @Test
        void testBroadcastSending() {
                MessageBusImpl messageBus = MessageBusImpl.getInstance();
                MicroService listener1 = new ExampleBroadcastListenerService("Listener1", new String[] { "5" });
                MicroService listener2 = new ExampleBroadcastListenerService("Listener2", new String[] { "5" });

                // Register services
                messageBus.register(listener1);
                messageBus.register(listener2);

                // Subscribe to broadcast
                messageBus.subscribeBroadcast(ExampleBroadcast.class, listener1);
                messageBus.subscribeBroadcast(ExampleBroadcast.class, listener2);

                // Send broadcast and verify reception
                Broadcast broadcast = new ExampleBroadcast("TestBroadcast");
                messageBus.sendBroadcast(broadcast);

                assertDoesNotThrow(() -> {
                        assertEquals(broadcast, messageBus.awaitMessage(listener1),
                                        "Listener1 should receive the broadcast.");
                        assertEquals(broadcast, messageBus.awaitMessage(listener2),
                                        "Listener2 should receive the broadcast.");
                });

                // Cleanup
                messageBus.unregister(listener1);
                messageBus.unregister(listener2);
        }

        @Test
        void testUnregisterAndRemoveReferences() {
                MessageBusImpl messageBus = MessageBusImpl.getInstance();
                MicroService service = new ExampleBroadcastListenerService("Service", new String[] { "5" });

                // Register and subscribe
                messageBus.register(service);
                messageBus.subscribeEvent(ExampleEvent.class, service);
                messageBus.subscribeBroadcast(ExampleBroadcast.class, service);

                // Verify registration and subscriptions
                assertTrue(messageBus.isRegistered(service), "Service should be registered.");
                assertTrue(messageBus.isSubscribedToEvent(ExampleEvent.class, service),
                                "Service should be subscribed to ExampleEvent.");
                assertTrue(messageBus.isSubscribedToBroad(ExampleBroadcast.class, service),
                                "Service should be subscribed to ExampleBroadcast.");

                // Unregister and verify
                messageBus.unregister(service);
                assertFalse(messageBus.isRegistered(service), "Service should no longer be registered.");
                assertFalse(messageBus.isSubscribedToEvent(ExampleEvent.class, service),
                                "Service should no longer be subscribed to ExampleEvent.");
                assertFalse(messageBus.isSubscribedToBroad(ExampleBroadcast.class, service),
                                "Service should no longer be subscribed to ExampleBroadcast.");

                // Ensure awaiting message throws an exception
                assertThrows(IllegalStateException.class, () -> messageBus.awaitMessage(service),
                                "Awaiting message for unregistered service should throw an exception.");
        }

        @Test
        void testEventSendingRoundRobin() throws InterruptedException {
                MessageBusImpl messageBus = MessageBusImpl.getInstance();
                MicroService handler1 = new ExampleEventHandlerService("Handler1", new String[] { "5" });
                MicroService handler2 = new ExampleEventHandlerService("Handler2", new String[] { "5" });

                // Register and subscribe
                messageBus.register(handler1);
                messageBus.register(handler2);
                messageBus.subscribeEvent(ExampleEvent.class, handler1);
                messageBus.subscribeEvent(ExampleEvent.class, handler2);

                // Send events
                Event<String> event1 = new ExampleEvent("Event1");
                Event<String> event2 = new ExampleEvent("Event2");
                Event<String> event3 = new ExampleEvent("Event3");
                messageBus.sendEvent(event1);
                messageBus.sendEvent(event2);
                messageBus.sendEvent(event3);

                // Verify round-robin behavior
                assertEquals(event1, messageBus.awaitMessage(handler1), "Event1 should be received by Handler1.");
                assertEquals(event2, messageBus.awaitMessage(handler2), "Event2 should be received by Handler2.");
                assertEquals(event3, messageBus.awaitMessage(handler1), "Event3 should be received by Handler1.");

                // Cleanup
                messageBus.unregister(handler1);
                messageBus.unregister(handler2);
        }

        @Test
        void testAwaitMessageThrowsExceptionForUnregisteredService() {
                MessageBusImpl messageBus = MessageBusImpl.getInstance();
                MicroService unregisteredService = new ExampleBroadcastListenerService("Unregistered",
                                new String[] { "5" });

                assertThrows(IllegalStateException.class, () -> {
                        messageBus.awaitMessage(unregisteredService);
                }, "Unregistered service should throw IllegalStateException when awaiting a message.");
        }

        @Test
        void testThreadSafety() throws InterruptedException {
                MessageBusImpl messageBus = MessageBusImpl.getInstance();
                int numServices = 20;
                ExecutorService executor = Executors.newFixedThreadPool(numServices);

                // Register services and subscribe to ExampleEvent
                List<MicroService> services = new ArrayList<>();
                for (int i = 0; i < numServices; i++) {
                        MicroService service = new ExampleEventHandlerService("Service" + i, new String[] { "5" });
                        messageBus.register(service);
                        messageBus.subscribeEvent(ExampleEvent.class, service);
                        services.add(service);
                }

                List<Event<String>> events = Arrays.asList(
                                new ExampleEvent("Event1"),
                                new ExampleEvent("Event2"),
                                new ExampleEvent("Event3"));

                // Send events in parallel
                for (Event<String> event : events) {
                        executor.submit(() -> {
                                Future<String> future = messageBus.sendEvent(event);
                                assertNotNull(future, "Future should not be null for a valid event.");
                        });
                }

                // Validate each service processes at most one event in round-robin
                Set<MicroService> processedServices = new HashSet<>();
                for (int i = 0; i < events.size(); i++) {
                        for (MicroService service : services) {
                                if (processedServices.size() == events.size())
                                        break;
                                Message message = messageBus.awaitMessage(service);
                                if (message instanceof ExampleEvent) {
                                        ExampleEvent receivedEvent = (ExampleEvent) message;
                                        assertTrue(events.contains(receivedEvent), "Received unexpected event.");
                                        processedServices.add(service);
                                }
                        }
                }

                // Ensure each event was processed by one service
                assertEquals(events.size(), processedServices.size(), "Each event should be processed by one service.");

                // Cleanup
                services.forEach(messageBus::unregister);
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
        }
}
