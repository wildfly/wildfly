package org.wildfly.as.concurrent.context;

import org.wildfly.as.concurrent.tasklistener.TaskListener;

import javax.enterprise.concurrent.AbortedException;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.SkippedException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

/**
 * The base for a contextual task, adapted into a {@link TaskListener}, to be executed at {@link org.wildfly.as.concurrent.tasklistener.TaskListenerExecutorService}s.
 *
 * @author Eduardo Martins
 */
abstract class ContextualTask implements TaskListener {

    private final ManagedTaskListener managedTaskListener;
    private final String identityName;
    private final ManagedExecutorService executor;
    private Future<?> future;

    /**
     * @param task
     * @param contextConfiguration
     * @param executor
     */
    ContextualTask(Object task, ContextConfiguration contextConfiguration, ManagedExecutorService executor) {
        if (task instanceof ManagedTask) {
            final ManagedTask managedTask = (ManagedTask) task;
            ManagedTaskConfiguration managedTaskConfiguration = new ManagedTaskConfiguration(managedTask);
            this.managedTaskListener = ContextualManagedTaskListener.getContextualManagedTaskListener(managedTaskConfiguration, contextConfiguration);
            this.identityName = managedTaskConfiguration.getIdentityName();
        } else {
            this.managedTaskListener = null;
            this.identityName = task.toString();
        }
        this.executor = executor;
    }

    /**
     * @return
     */
    String getIdentityName() {
        return identityName;
    }

    @Override
    public void taskSubmitted(Future<?> future) {
        this.future = future;
        if (managedTaskListener != null) {
            managedTaskListener.taskSubmitted(future, executor);
        }
    }

    @Override
    public void taskStarting() {
        if (managedTaskListener != null) {
            managedTaskListener.taskStarting(future, executor);
        }
    }

    @Override
    public void taskDone(Throwable exception) {
        if (managedTaskListener != null) {
            if (exception != null) {
                if (!(exception instanceof SkippedException) && !(exception instanceof CancellationException)) {
                    exception = new AbortedException(exception);
                }
                managedTaskListener.taskAborted(future, executor, exception);
            }
            managedTaskListener.taskDone(future, executor, exception);
        }
    }

    @Override
    public String toString() {
        // do not change this, specs define task.toString() as fallback to obtain task identity name, here we ensure the fallback is consistent with wrapped task, in case it's a spec ManagedTask
        return identityName;
    }
}
