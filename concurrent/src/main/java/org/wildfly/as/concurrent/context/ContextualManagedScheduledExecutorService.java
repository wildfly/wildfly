package org.wildfly.as.concurrent.context;

import org.wildfly.as.concurrent.tasklistener.TaskListenerScheduledExecutorService;

import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.Trigger;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ManagedScheduledExecutorService} which wraps tasks so these get invoked with a specific context set, and delegates executions into a {@link TaskListenerScheduledExecutorService}.
 *
 * @author Eduardo Martins
 */
public class ContextualManagedScheduledExecutorService extends ContextualManagedExecutorService implements ManagedScheduledExecutorService {

    private final TaskListenerScheduledExecutorService scheduledExecutorService;

    public ContextualManagedScheduledExecutorService(TaskListenerScheduledExecutorService scheduledExecutorService, ContextConfiguration contextConfiguration) {
        super(scheduledExecutorService, contextConfiguration);
        this.scheduledExecutorService = scheduledExecutorService;
    }

    // ScheduledExecutorService delegation

    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduledExecutorService.schedule(wrap(task), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
        return scheduledExecutorService.schedule(wrap(task), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduledExecutorService.scheduleAtFixedRate(wrap(task), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        return scheduledExecutorService.scheduleWithFixedDelay(wrap(task), initialDelay, delay, unit);
    }

    // ManagedScheduledExecutorService impl

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> task, Trigger trigger) {
        return new TriggerScheduledFuture<>(task, trigger, this);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        return new TriggerScheduledFuture<>(task, trigger, this);
    }

}
