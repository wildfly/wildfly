package org.wildfly.as.concurrent.context;

import javax.enterprise.concurrent.ManagedExecutorService;
import java.util.concurrent.Callable;

/**
 * A {@link Callable} {@link ContextualTask}.
 *
 * @author Eduardo Martins
 */
class ContextualTaskCallable<V> extends ContextualTask implements Callable<V> {

    private final Context context;
    private final Callable<V> task;

    ContextualTaskCallable(Callable<V> task, ContextConfiguration contextConfiguration, ManagedExecutorService executor) {
        super(task, contextConfiguration, executor);
        this.task = task;
        this.context = contextConfiguration != null ? contextConfiguration.newTaskContext(task) : null;
    }

    @Override
    public V call() throws Exception {
        final Context previousContext = Utils.setContext(context);
        try {
            return task.call();
        } finally {
            Utils.setContext(previousContext);
        }
    }

}
