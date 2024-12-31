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

    private static class SingletonHolderMessageBusImpl { // מימוש כמו שהוצג בכיתה
        private static final MessageBusImpl INSTANCE = new MessageBusImpl();
    }

    public static MessageBusImpl getInstance() {
        return SingletonHolderMessageBusImpl.INSTANCE;
    }

    @Override
    public void register(MicroService m) {
        microServiceQueues.putIfAbsent(m, new LinkedBlockingQueue<>());
    }

    @Override
    public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
        eventSubscribers.putIfAbsent(type, new LinkedList<>());
        Queue<MicroService> subscribers = eventSubscribers.get(type);
        synchronized (subscribers) {
            subscribers.add(m);
        }
    }

    @Override
    public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
        broadcastSubscribers.putIfAbsent(type, new ArrayList<>());
        List<MicroService> subscribers = broadcastSubscribers.get(type);
        subscribers.add(m);

    }

    /**
     * מעדכן את ה-Future של האירוע, כשהוא מקבל את תוצאת הביצוע.
     * משתמש במידע שנשלח כדי להחזיר תוצאה ל-Future המתאימה.
     */
    @Override
    public <T> void complete(Event<T> e, T result) {
        @SuppressWarnings("unchecked")
        Future<T> future = (Future<T>) eventFutures.get(e);
        if (future != null) {
            future.resolve(result); // עדכון התוצאה
        }
    }

    @Override
    public void sendBroadcast(Broadcast b) {
        // שלוף את רשימת המנויים לשידור מסוג זה
        List<MicroService> subscribers = broadcastSubscribers.get(b.getClass());

        // בדוק אם קיימים מנויים
        if (subscribers != null && !subscribers.isEmpty()) {
            for (MicroService m : subscribers) {
                try {
                    // שלוף את התור של המיקרו-שירות
                    BlockingQueue<Message> queue = microServiceQueues.get(m);

                    // בדוק אם התור קיים
                    if (queue == null) {
                        throw new IllegalStateException("Queue for the MicroService does not exist.");
                    }

                    // הכנס את ההודעה לתור
                    queue.put(b);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // שחזר את מצב ה-interrupt
                    e.printStackTrace(); // הדפס את ה-stack trace לצורכי דיבוג
                } catch (IllegalStateException ex) {
                    System.err.println("Error: " + ex.getMessage()); // הדפס הודעת שגיאה
                }
            }
        } else {
            System.out.println("No subscribers found for the broadcast: " + b.getClass().getName());
        }
    }

    /**
     * שולח אירוע למיקרו-שירות שנרשם אליו (אם יש מנוי).
     * אם יש מנויים, האירוע נשלח לפי עקרון round-robin (ברירת מחדל: המיקרו-שירות
     * הראשון).
     */
    @Override
    public <T> Future<T> sendEvent(Event<T> e) {
        // בודק אם יש מנויים לאירוע מסוג זה
        Queue<MicroService> subscribers = eventSubscribers.get(e.getClass());
        if (subscribers == null || subscribers.isEmpty()) { // אם אין מנויים
            return null; // החזר null במקום לנסות לגשת ל-subscribe null
        }
        // אם אין מנויים, מחזיר null
        MicroService selectedService;
        synchronized (subscribers) {
            // בוחר מיקרו-שירות לשלוח אליו את האירוע (בחרנו כאן את הראשון ברשימה)
            selectedService = subscribers.poll(); // במימוש זה, בחרנו את המיקרו-שירות הראשון
            if (selectedService != null) {
                subscribers.add(selectedService); // מעביר אותו לסוף התור
            } else {
                return null; // אין מנויים זמינים
            }
        }
        Future<T> future = new Future<>();
        // שומרים את ה-Future של האירוע כך שנוכל להחזיר את התוצאה בהמשך
        eventFutures.putIfAbsent(e, future);
        try {
            // : בדוק אם תור ה-MicroService קיים לפני הכנסת ההודעה
            BlockingQueue<Message> queue = microServiceQueues.get(selectedService);
            if (queue == null) { // אם התור חסר, זרוק שגיאה
                throw new IllegalStateException("Queue for the MicroService does not exist.");
            }
            queue.put(e); // הכנס את האירוע לתור
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); // שחזר את מצב ה-interrupt
            ex.printStackTrace(); // טיפול בשגיאת InterruptedException
        }

        // מחזירים את ה-Future של האירוע
        return future;
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

    /**
     * מחפש את ההודעה הבאה בתור של המיקרו-שירות וממתין לה אם אין.
     * במקרה שאין הודעה, המיקרו-שירות יחכה עד שתהיה אחת.
     */

    @Override
    public Message awaitMessage(MicroService m) throws InterruptedException {
        // בדוק אם המיקרו-שירות רשום
        if (!microServiceQueues.containsKey(m)) {
            throw new IllegalStateException("MicroService is not registered with the MessageBus.");
        }
        // אחזר את התור של המיקרו-שירות
        BlockingQueue<Message> queue = microServiceQueues.get(m);
        // אם התור לא קיים (לא סביר), זרוק שגיאה
        if (queue == null) {
            throw new IllegalStateException("Queue for the MicroService is missing.");
        }

        // חכה להודעה בתור (פעולה חסימתית)
        return queue.take(); // מחכה עד שמגיעה הודעה
    }

    public Map<MicroService, BlockingQueue<Message>> getMicroServiceQueues() {
        return microServiceQueues;
    }

}