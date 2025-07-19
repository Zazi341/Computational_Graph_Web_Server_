package graph;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class ObservableFuture<V> {
    private final Future<V> future;
    private final List<Observer<V>> observers = new ArrayList<>();
    private boolean notified = false;

    // Constructor
    public ObservableFuture(Future<V> future) {
        this.future = future;
    }

    // Add an observer to the list
    public void addObserver(Observer<V> observer) {
        synchronized (observers) {
            observers.add(observer);
        }
        // Check if already notified and notify the new observer immediately
        if (notified) {
            try {
                V value = future.get();
                observer.onValueReady(this, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Get the value if ready, otherwise return null
    public V get() {
        if (future.isDone()) {
            try {
                return future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null; // Return null if not ready
    }

    // Notify all observers when the value is ready
    private void notifyObservers(V value) {
        synchronized (observers) {
            notified = true;
            for (Observer<V> observer : observers) {
                observer.onValueReady(this, value);
            }
        }
    }

    // Background thread to notify observers once the value is ready
    public void startObserverNotifier() {
        new Thread(() -> {
            try {
                V value = future.get(); // Wait for the value
                notifyObservers(value); // Notify observers when ready
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Observer interface
    public interface Observer<V> {
        void onValueReady(ObservableFuture<V> source, V value);
    }
}

