package bgu.spl;

import org.junit.jupiter.api.*; // עבור Test, BeforeEach, AfterEach
import java.util.concurrent.TimeUnit; // עבור Future והמתנה עם timeout
import static org.junit.jupiter.api.Assertions.*; // עבור Assertions כמו assertEquals, assertNotNull
import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.Event;
import bgu.spl.mics.Broadcast;
import bgu.spl.mics.Message;
import bgu.spl.mics.Future;

class MessageBusImplTest {

    private MessageBusImpl messageBus;
    private MicroService testMicroService;

    @BeforeEach
    void setUp() {
        messageBus = MessageBusImpl.getInstance();
        testMicroService = new MicroService("TestMicroService") {
            @Override
            protected void initialize() {
                // Dummy initialization
            }
        };
        messageBus.register(testMicroService); // רישום המיקרו-שירות לפני כל טסט
    }

    @AfterEach
    void tearDown() {
        messageBus.unregister(testMicroService); // ניקוי אחרי כל טסט
    }

    @Test
    void testRegisterAndAwaitMessage() throws InterruptedException {
        // בדוק שהמיקרו-שירות רשום כראוי ויכול לקבל הודעות
        class TestEvent implements Event<String> {
        }
        messageBus.subscribeEvent(TestEvent.class, testMicroService);

        Event<String> event = new TestEvent();
        messageBus.sendEvent(event);

        Message received = messageBus.awaitMessage(testMicroService);
        assertNotNull(received, "Message should not be null");
        assertEquals(event, received, "Message should match the event sent");
    }

    @Test
    void testSendBroadcast() throws InterruptedException {
        // בדוק שליחת Broadcast למיקרו-שירות
        class TestBroadcast implements Broadcast {
        }
        messageBus.subscribeBroadcast(TestBroadcast.class, testMicroService);

        Broadcast broadcast = new TestBroadcast();
        messageBus.sendBroadcast(broadcast);

        Message received = messageBus.awaitMessage(testMicroService);
        assertNotNull(received, "Message should not be null");
        assertTrue(received instanceof TestBroadcast, "Message should be of type TestBroadcast");
    }

    @Test
    void testSendEventToMultipleSubscribers() throws InterruptedException {
        // בדוק שליחת אירוע למספר מנויים בסבב (round-robin)
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
        messageBus.sendEvent(event2);

        // בדוק שהאירועים נשלחו לשני המיקרו-שירותים בסבב
        assertEquals(event1, messageBus.awaitMessage(testMicroService));
        assertEquals(event2, messageBus.awaitMessage(secondMicroService));

        messageBus.unregister(secondMicroService); // ניקוי
    }

    @Test
    void testCompleteEvent() throws InterruptedException {
        // בדוק השלמת Future לאירוע
        class TestEvent implements Event<String> {
        }
        messageBus.subscribeEvent(TestEvent.class, testMicroService);

        Event<String> event = new TestEvent();
        Future<String> future = messageBus.sendEvent(event);

        assertNotNull(future, "Future should not be null after sending an event");

        messageBus.complete(event, "Result");
        assertEquals("Result", future.get(1, TimeUnit.SECONDS), "Future result should match the completed value");
    }

    @Test
    void testAwaitMessageBlocking() {
        // רישום המיקרו-שירות
        class TestEvent implements Event<String> {
        }
        messageBus.register(testMicroService);

        // יצירת Thread שמנסה לחכות להודעה
        Thread thread = new Thread(() -> {
            try {
                messageBus.awaitMessage(testMicroService);
                fail("awaitMessage should block until a message is available");
            } catch (InterruptedException ignored) {
                // Expected behavior when interrupted
            }
        });

        thread.start();

        try {
            Thread.sleep(100); // המתן זמן קצר לוודא שה-Thread נחסם
            assertTrue(thread.isAlive(), "Thread should be blocked on awaitMessage");
        } catch (InterruptedException ignored) {
        }

        // וודא שהמיקרו-שירות נרשם ושיש תור
        assertNotNull(messageBus.getMicroServiceQueues().get(testMicroService),
                "MicroService queue should exist after registration");

        // שחרר את ה-Thread על ידי שליחת הודעה
        messageBus.sendEvent(new TestEvent());

        try {
            thread.join(); // המתן לסיום ה-thread
        } catch (InterruptedException ignored) {
        }
    }
}