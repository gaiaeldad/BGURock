package bgu.spl.mics;

import java.util.*;
import java.util.concurrent.*;



/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only private fields and methods can be added to this class.
 */
    public class MessageBusImpl implements MessageBus {
        private final Map<Class<? extends Event<?>>, Queue<MicroService>> eventSubscribers = new ConcurrentHashMap<>();
        private final Map<Class<? extends Broadcast>, List<MicroService>> broadcastSubscribers = new ConcurrentHashMap<>();
        private final Map<Event<?>, Future<?>> eventFutures = new ConcurrentHashMap<>();
        private final Map<MicroService, BlockingQueue<Message>> microServiceEventQueues = new ConcurrentHashMap<>();
        private final Map<MicroService, BlockingQueue<Message>> microServiceBroadcastQueues = new ConcurrentHashMap<>();

        private static class SingletonHolderMessageBusImpl { // מימוש כמו שהוצג בכיתה
            private static final MessageBusImpl INSTANCE = new MessageBusImpl();
        }
    
        public static MessageBusImpl getInstance() {
            return SingletonHolderMessageBusImpl.INSTANCE;
        }

        @Override
        public void register(MicroService m) {
            microServiceEventQueues.putIfAbsent(m, new LinkedBlockingQueue<>());
            microServiceBroadcastQueues.putIfAbsent(m, new LinkedBlockingQueue<>());
        }

        @Override
        public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
            eventSubscribers.putIfAbsent(type, new LinkedList<>());
            Queue <MicroService> subscribers = eventSubscribers.get(type);
            synchronized(subscribers){
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
            List<MicroService> subscribers;
            synchronized(broadcastSubscribers){
                subscribers = broadcastSubscribers.get(b.getClass());
            }
            
            if (subscribers != null) {
                    for (MicroService m : subscribers) {
                        try {
                            microServiceBroadcastQueues.get(m).put(b);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                
            }
        }

        
        /**
         * שולח אירוע למיקרו-שירות שנרשם אליו (אם יש מנוי).
         * אם יש מנויים, האירוע נשלח לפי עקרון round-robin (ברירת מחדל: המיקרו-שירות הראשון).
         */
        @Override
        public <T> Future<T> sendEvent(Event<T> e) {
            // בודק אם יש מנויים לאירוע מסוג זה
            Queue <MicroService> subscribers = eventSubscribers.get(e.getClass());
            // אם אין מנויים, מחזיר null
            MicroService selectedService;
            synchronized(subscribers){
                if (subscribers == null || subscribers.isEmpty()) {
                    return null;
                }

                // בוחר מיקרו-שירות לשלוח אליו את האירוע (בחרנו כאן את הראשון ברשימה)
                selectedService = subscribers.poll(); // במימוש זה, בחרנו את המיקרו-שירות הראשון
                if (selectedService != null) {
                    subscribers.add(selectedService); // מעביר אותו לסוף התור
                }
            }
            Future<T> future = new Future<>();
            
            // שומרים את ה-Future של האירוע כך שנוכל להחזיר את התוצאה בהמשך
            eventFutures.putIfAbsent(e, future);
            try {
                // שולחים את האירוע למיקרו-שירות הנבחר
                microServiceEventQueues.get(selectedService).put(e);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            // מחזירים את ה-Future של האירוע
            return future;
        }

    
        

        @Override
        public void unregister(MicroService m) {
                microServiceEventQueues.remove(m); 
                microServiceBroadcastQueues.remove(m); 
                for (Queue <MicroService> subscribers : eventSubscribers.values()) {
                    synchronized(subscribers){
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
            BlockingQueue<Message> eventQueue = microServiceEventQueues.get(m);
            BlockingQueue<Message> broadcastQueue = microServiceBroadcastQueues.get(m);
            while (true) {
                Message broadcastMessage = broadcastQueue.poll();
                if (broadcastMessage != null) {
                    return broadcastMessage;
                }
                Message eventMessage = eventQueue.poll();
                if (eventMessage != null) {
                    return eventMessage;
                }
                Thread.sleep(100); //לבדוק
            }
        
        }
    


	

}