package bgu.spl.mics;

import java.util.concurrent.TimeUnit;

/**
 * A Future object represents a promised result - an object that will
 * eventually be resolved to hold a result of some operation. The class allows
 * Retrieving the result once it is available.
 * 
 * Only private methods may be added to this class.
 * No public constructor is allowed except for the empty constructor.
 */

public class Future<T> {
   private volatile T result = null;
   private volatile boolean isResolved = false;
   private final Object lock = new Object();

   public Future() {
   this.result = null;  // תוצאה לא מוגדרת בהתחלה
   this.isResolved = false;  // עדיין לא "נפתר"
}

   public T get() {
      if (result == null) { 
         synchronized (lock) {
            while (!isDone()) {  // Block the thread until the result is available.
               try {
                  lock.wait();
               } catch (InterruptedException e) {
                  Thread.currentThread().interrupt(); // Handle interruption.
               }
            }
            return result; 
         }
      }
      return result;
   }

    public void resolve(T result) {
      if (this.result == null){
         synchronized (lock) {
            if (!isDone()) {  // Check if already resolved using isDone()
               this.result = result;
               isResolved = true;
               lock.notifyAll(); // Notify all threads waiting for the result.
            }
        }
      }
    }
 

   public boolean isDone() {// אולי למחוק סנכרון
      synchronized (lock) {
         return isResolved;
      }
   }

   public T get(long timeout, TimeUnit unit) {
      if (result == null){
         synchronized (lock) {
            if (result == null){
               long millisTimeout = unit.toMillis(timeout);
               long startTime = System.currentTimeMillis();
               while (!isDone()) {
                  long elapsedTime = System.currentTimeMillis() - startTime;
                  long remainingTime = millisTimeout - elapsedTime;
                  if (remainingTime <= 0L) {
                     return null;
                  }
   
                  try {
                     lock.wait(remainingTime);
                  } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                  }
               }
            }
            return result;
         }
      }
      return result;
   }
}
