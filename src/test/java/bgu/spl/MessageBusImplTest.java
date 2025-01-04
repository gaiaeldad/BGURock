package bgu.spl;

import bgu.spl.mics.*;
import bgu.spl.mics.example.messages.ExampleBroadcast;
import bgu.spl.mics.example.messages.ExampleEvent;
import bgu.spl.mics.example.services.ExampleBroadcastListenerService;
import bgu.spl.mics.example.services.ExampleEventHandlerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class MessageBusImplTest {

        /**
         * Test: Register and subscribe a microservice to broadcast messages.
         * Pre-Condition: Microservice is not registered and has no subscriptions.
         * Post-Condition: Microservice is registered and subscribed to broadcast.
         * Invariant: No duplicate registrations or subscriptions.
         */
        @Test
        void testRegisterBroadcastSubscription() {
                // Setup
                MessageBusImpl messageBus = MessageBusImpl.getInstance();
                MicroService listener = new ExampleBroadcastListenerService("Listener", new String[] { "5" });

                // Pre-Condition
                assertFalse(messageBus.isRegistered(listener), "Microservice should not be registered initially.");

                // Action: Registration
                messageBus.register(listener);
                assertTrue(messageBus.isRegistered(listener), "Microservice should be registered.");

                // Invariant: No duplicate registrations
                messageBus.register(listener);
                assertEquals(1, messageBus.getNumberOfRegisters(), "Microservice should only be registered once.");

                // Action: Subscription
                messageBus.subscribeBroadcast(ExampleBroadcast.class, listener);
                assertTrue(messageBus.isSubscribedToBroad(ExampleBroadcast.class, listener),
                                "Microservice should be subscribed to broadcast.");

                // Invariant: No duplicate subscriptions
                messageBus.subscribeBroadcast(ExampleBroadcast.class, listener);
                assertEquals(1, messageBus.getNumberOfSubscribersToBroad(ExampleBroadcast.class),
                                "Microservice should only be subscribed once to broadcast.");

                // Post-Condition: Unregister
                messageBus.unregister(listener);
                assertFalse(messageBus.isRegistered(listener), "Microservice should be unregistered.");
                assertFalse(messageBus.isSubscribedToBroad(ExampleBroadcast.class, listener),
                                "Microservice should not be subscribed after unregister.");
        }

        /**
         * Test: Register and subscribe a microservice to event messages.
         * Pre-Condition: Microservice is not registered and has no subscriptions.
         * Post-Condition: Microservice is registered and subscribed to event.
         * Invariant: No duplicate registrations or subscriptions.
         */
        @Test
        void testRegisterEventSubscription() {
                // Setup
                MessageBusImpl messageBus = MessageBusImpl.getInstance();
                MicroService handler = new ExampleEventHandlerService("Handler", new String[] { "5" });

                // Pre-Condition
                assertFalse(messageBus.isRegistered(handler), "Microservice should not be registered initially.");

                // Action: Registration
                messageBus.register(handler);
                assertTrue(messageBus.isRegistered(handler), "Microservice should be registered.");

                // Invariant: No duplicate registrations
                messageBus.register(handler);
                assertEquals(1, messageBus.getNumberOfRegisters(), "Microservice should only be registered once.");

                // Action: Subscription
                messageBus.subscribeEvent(ExampleEvent.class, handler);
                assertTrue(messageBus.isSubscribedToEvent(ExampleEvent.class, handler),
                                "Microservice should be subscribed to event.");

                // Invariant: No duplicate subscriptions
                messageBus.subscribeEvent(ExampleEvent.class, handler);
                assertEquals(1, messageBus.getNumberOfSubscribersToEvent(ExampleEvent.class),
                                "Microservice should only be subscribed once to event.");

                // Post-Condition: Unregister
                messageBus.unregister(handler);
                assertFalse(messageBus.isRegistered(handler), "Microservice should be unregistered.");
                assertFalse(messageBus.isSubscribedToEvent(ExampleEvent.class, handler),
                                "Microservice should not be subscribed after unregister.");
        }

        /**
         * Test: Send broadcast messages to multiple subscribers.
         * Pre-Condition: Two microservices registered and subscribed to broadcast.
         * Post-Condition: Both microservices receive the broadcast message.
         * Invariant: Message order and delivery are consistent.
         */
        @Test
        void testSendBroadcast() {
                // Setup
                MessageBusImpl messageBus = MessageBusImpl.getInstance();
                MicroService listener1 = new ExampleBroadcastListenerService("Listener1", new String[] { "5" });
                MicroService listener2 = new ExampleBroadcastListenerService("Listener2", new String[] { "5" });

                // Pre-Condition
                messageBus.register(listener1);
                messageBus.register(listener2);
                messageBus.subscribeBroadcast(ExampleBroadcast.class, listener1);
                messageBus.subscribeBroadcast(ExampleBroadcast.class, listener2);

                // Action: Send broadcast
                Broadcast broadcast = new ExampleBroadcast("TestBroadcast");
                messageBus.sendBroadcast(broadcast);

                // Post-Condition: Check message delivery
                assertDoesNotThrow(() -> {
                        assertEquals(broadcast, messageBus.awaitMessage(listener1),
                                        "Listener1 did not receive the broadcast.");
                        assertEquals(broadcast, messageBus.awaitMessage(listener2),
                                        "Listener2 did not receive the broadcast.");
                });

                // Cleanup
                messageBus.unregister(listener1);
                messageBus.unregister(listener2);
                assertFalse(messageBus.isRegistered(listener1), "Listener1 should be unregistered.");
                assertFalse(messageBus.isRegistered(listener2), "Listener2 should be unregistered.");
        }

}
