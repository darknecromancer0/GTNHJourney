package dev.gtnhjourney.client;

import java.util.ArrayDeque;
import java.util.Deque;

/** Small FIFO used to move network callbacks onto the client tick thread in legacy Forge. */
public final class ClientTaskQueue {

    public interface FailureHandler {

        void failed(Throwable failure);
    }

    private final Deque<Runnable> tasks = new ArrayDeque<Runnable>();

    public synchronized void enqueue(Runnable task) {
        if (task == null) throw new IllegalArgumentException("task must not be null");
        tasks.addLast(task);
    }

    /** Executes at most {@code maxTasks} tasks in FIFO order on the caller thread. */
    public int drain(int maxTasks) {
        if (maxTasks <= 0) return 0;
        int executed = 0;
        while (executed < maxTasks) {
            Runnable task;
            synchronized (this) {
                task = tasks.pollFirst();
            }
            if (task == null) break;
            task.run();
            executed++;
        }
        return executed;
    }

    /** Like {@link #drain(int)}, but one broken legacy/mod task cannot abort the remaining FIFO. */
    public int drainSafely(int maxTasks, FailureHandler failureHandler) {
        if (maxTasks <= 0) return 0;
        int executed = 0;
        while (executed < maxTasks) {
            Runnable task;
            synchronized (this) {
                task = tasks.pollFirst();
            }
            if (task == null) break;
            try {
                task.run();
            } catch (RuntimeException failure) {
                reportFailure(failureHandler, failure);
            } catch (LinkageError failure) {
                reportFailure(failureHandler, failure);
            }
            executed++;
        }
        return executed;
    }

    private static void reportFailure(FailureHandler handler, Throwable failure) {
        if (handler == null) return;
        try {
            handler.failed(failure);
        } catch (RuntimeException ignored) {} catch (LinkageError ignored) {}
    }

    public synchronized int size() {
        return tasks.size();
    }

    public synchronized void clear() {
        tasks.clear();
    }
}
