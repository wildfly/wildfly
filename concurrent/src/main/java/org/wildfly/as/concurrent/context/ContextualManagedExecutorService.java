package org.wildfly.as.concurrent.context;

import org.wildfly.as.concurrent.tasklistener.TaskListenerExecutorService;

import javax.enterprise.concurrent.ManagedExecutorService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link ManagedExecutorService} which wraps tasks so these get invoked with a specific context set, and delegates executions into a {@link TaskListenerExecutorService}.
 *
 * @author Eduardo Martins
 */
public class ContextualManagedExecutorService implements ManagedExecutorService {

    private final TaskListenerExecutorService taskListenerExecutorService;
    private final ContextConfiguration contextConfiguration;

    public ContextualManagedExecutorService(TaskListenerExecutorService taskListenerExecutorService, ContextConfiguration contextConfiguration) {
        this.taskListenerExecutorService = taskListenerExecutorService;
        this.contextConfiguration = contextConfiguration;
    }

    public ContextConfiguration getContextConfiguration() {
        return contextConfiguration;
    }

    public TaskListenerExecutorService getTaskListenerExecutorService() {
        return taskListenerExecutorService;
    }

    // ExecutorService delegation

    @Override
    public void shutdown() {
        taskListenerExecutorService.shutdown();
    }

    @Override
    public List<java.lang.Runnable> shutdownNow() {
        return taskListenerExecutorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return taskListenerExecutorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return taskListenerExecutorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return taskListenerExecutorService.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return taskListenerExecutorService.submit(wrap(task));
    }

    @Override
    public <T> Future<T> submit(java.lang.Runnable task, T result) {
        return taskListenerExecutorService.submit(wrap(task), result);
    }

    @Override
    public Future<?> submit(java.lang.Runnable task) {
        return taskListenerExecutorService.submit(wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return taskListenerExecutorService.invokeAll(wrap(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return taskListenerExecutorService.invokeAll(wrap(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return taskListenerExecutorService.invokeAny(wrap(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return taskListenerExecutorService.invokeAny(wrap(tasks), timeout, unit);
    }

    @Override
    public void execute(java.lang.Runnable task) {
        taskListenerExecutorService.execute(wrap(task));
    }

    // task wrapping

    protected Runnable wrap(Runnable task) {
        if (task instanceof ContextualTask) {
            return task;
        }
        return new ContextualTaskRunnable(task, contextConfiguration, this);
    }

    protected <V> Callable<V> wrap(Callable<V> task) {
        if (task instanceof ContextualTask) {
            return task;
        }
        return new ContextualTaskCallable<>(task, contextConfiguration, this);
    }

    protected <T> Collection<? extends Callable<T>> wrap(Collection<? extends Callable<T>> tasks) {
        final List<Callable<T>> wrappedTasks = new ArrayList<>();
        for (Callable<T> callable : tasks) {
            wrappedTasks.add(wrap(callable));
        }
        return wrappedTasks;
    }

}
