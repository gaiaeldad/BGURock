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
    private volatile T result; // The result of the computation.
    private volatile boolean isResolved; // Whether the result is resolved.

    private final Object resultLock = new Object(); // מנעול לעדכון התוצאה
    private final Object statusLock = new Object(); // מנעול לבדיקה/עדכון מצב

    public Future() {
        this.result = null;       
        this.isResolved = false;  
    }

    public T get() {
        synchronized (resultLock) {
            while (!isDone()) {
                try {
                    resultLock.wait(); // Block the thread until the result is available.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Handle interruption.
                }
            }
            return result; // Return the resolved result.
        }
    }

    public void resolve(T result) {
        synchronized (resultLock) {
            if (!isDone()) { // Make sure we resolve only once.
                this.result = result;
                synchronized (statusLock) {
                    this.isResolved = true;
                }
                resultLock.notifyAll(); // Notify all threads waiting for the result.
            }
        }
    }

    public boolean isDone() {
        synchronized (statusLock) {
            return isResolved;
        }
    }

    public T get(long timeout, TimeUnit unit) {
        synchronized (resultLock) {
            long millisTimeout = unit.toMillis(timeout); 
            long startTime = System.currentTimeMillis(); 

            while (!isDone()) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                long remainingTime = millisTimeout - elapsedTime;

                if (remainingTime <= 0) {
                    return null; // Timeout expired, return null.
                }

                try {
                    resultLock.wait(remainingTime); // Wait for the result or the timeout.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Handle interruption.
                }
            }
            return result; // Return the resolved result.
        }
    }
    public void setResult(T result) {
        this.result = result;
        setIsResolved(true);
    }

    // עדכון מצב ה-"הושלם"
    public void setIsResolved(boolean isResolved) {
        this.isResolved = isResolved;
    }
}
