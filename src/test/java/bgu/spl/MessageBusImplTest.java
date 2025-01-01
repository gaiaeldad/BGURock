package bgu.spl;

import org.junit.jupiter.api.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import bgu.spl.mics.*;
import org.junit.jupiter.api.Test; // JUnit 5

class MessageBusImplTest {

    private static final Logger logger = Logger.getLogger(MessageBusImplTest.class.getName());
    private MessageBusImpl messageBus;
    private MicroService testMicroService;

    @BeforeEach
    void setUp() {
        logger.info("Setting up MessageBus and registering testMicroService");
        messageBus = MessageBusImpl.getInstance();
        testMicroService = new MicroService("TestMicroService") {
            @Override
            protected void initialize() {
                // Dummy initialization
            }
        };
        messageBus.register(testMicroService);
    }

    @AfterEach
    void tearDown() {
        logger.info("Tearing down testMicroService");
        messageBus.unregister(testMicroService);
    }

    @Test
    void testRegisterAndAwaitMessage() throws InterruptedException {
        logger.info("Testing register and awaitMessage functionality");

        class TestEvent implements Event<String> {
        }
        messageBus.subscribeEvent(TestEvent.class, testMicroService);

        Event<String> event = new TestEvent();
        messageBus.sendEvent(event);

        Message received = messageBus.awaitMessage(testMicroService);
        logger.info("Received message: " + received);
        assertNotNull(received, "Message should not be null");
        assertEquals(event, received, "Message should match the event sent");
    }

    @Test
    void testSendBroadcast() throws InterruptedException {
        logger.info("Testing sendBroadcast functionality");

        class TestBroadcast implements Broadcast {
        }
        messageBus.subscribeBroadcast(TestBroadcast.class, testMicroService);

        Broadcast broadcast = new TestBroadcast();
        messageBus.sendBroadcast(broadcast);

        Message received = messageBus.awaitMessage(testMicroService);
        logger.info("Received broadcast: " + received);
        assertNotNull(received, "Message should not be null");
        assertTrue(received instanceof TestBroadcast, "Message should be of type TestBroadcast");
    }

    @Test
    void testSendEventToMultipleSubscribers() throws InterruptedException {
        logger.info("Testing round-robin event distribution to multiple subscribers");

        class TestEvent implements Event<String> {
        }

        MicroService secondMicroService = new MicroService("SecondMicroService") {
            @Override
            protected void initialize() {
            }
        };

        messageBus.register(secondMicroService);
        messageBus.subscribeEvent(TestEvent.class, testMicroService);
        messageBus.subscribeEvent(TestEvent.class, secondMicroService);

        Event<String> event1 = new TestEvent();
        Event<String> event2 = new TestEvent();

        messageBus.sendEvent(event1);
        logger.info("Sent first event: " + event1);
        messageBus.sendEvent(event2);
        logger.info("Sent second event: " + event2);

        assertEquals(event1, messageBus.awaitMessage(testMicroService));
        assertEquals(event2, messageBus.awaitMessage(secondMicroService));

        messageBus.unregister(secondMicroService);
    }

    @Test
    void testCompleteEvent() throws InterruptedException {
        logger.info("Testing complete functionality for events");

        class TestEvent implements Event<String> {
        }
        messageBus.subscribeEvent(TestEvent.class, testMicroService);

        Event<String> event = new TestEvent();
        Future<String> future = messageBus.sendEvent(event);

        logger.info("Future created for event: " + event);
        assertNotNull(future, "Future should not be null after sending an event");

        messageBus.complete(event, "Result");
        logger.info("Completed event with result: Result");

        assertEquals("Result", future.get(1, TimeUnit.SECONDS), "Future result should match the completed value");
    }

    @Test
    void testAwaitMessageBlocking() {
        logger.info("Testing awaitMessage blocking behavior");

        class TestEvent implements Event<String> {
        }

        messageBus.register(testMicroService);
        messageBus.subscribeEvent(TestEvent.class, testMicroService);

        Thread thread = new Thread(() -> {
            try {
                logger.info("Thread waiting on awaitMessage");
                messageBus.awaitMessage(testMicroService);
                logger.info("Thread unblocked after receiving message");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warning("Thread interrupted");
            }
        });

        thread.start();

        try {
            Thread.sleep(100);
            assertTrue(thread.isAlive(), "Thread should be blocked on awaitMessage");
            logger.info("Verified thread is blocked");
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }

        messageBus.sendEvent(new TestEvent());
        logger.info("Sent event to unblock thread");

        try {
            thread.join(500);
            assertFalse(thread.isAlive(), "Thread should be unblocked after message is available");
            logger.info("Thread successfully unblocked");
        } catch (InterruptedException e) {
            fail("Thread did not unblock properly");
        }
    }

    @Test
    void testUnregisterRemovesAllReferences() {
        logger.info("Testing unregister removes all references");

        class TestEvent implements Event<String> {
        }
        messageBus.subscribeEvent(TestEvent.class, testMicroService);

        messageBus.unregister(testMicroService);

        Event<String> event = new TestEvent();
        assertThrows(IllegalStateException.class, () -> messageBus.sendEvent(event));
        logger.info("Verified event cannot be sent to unregistered service");
    }

    @Test
    void testBroadcastWithoutSubscribers() {
        logger.info("Testing broadcast without subscribers");

        class TestBroadcast implements Broadcast {
        }
        Broadcast broadcast = new TestBroadcast();

        assertDoesNotThrow(() -> messageBus.sendBroadcast(broadcast));
        logger.info("Broadcast sent successfully with no subscribers");
    }
}
