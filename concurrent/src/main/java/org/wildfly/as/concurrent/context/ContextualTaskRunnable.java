package org.wildfly.as.concurrent.context;

import javax.enterprise.concurrent.ManagedExecutorService;

/**
 * A {@link Runnable} {@link ContextualTask}.
 *
 * @author Eduardo Martins
 */
class ContextualTaskRunnable extends ContextualTask implements Runnable {

    private final Context context;
    private final Runnable task;

    ContextualTaskRunnable(Runnable task, ContextConfiguration contextConfiguration, ManagedExecutorService executor) {
        super(task, contextConfiguration, executor);
        this.task = task;
        this.context = contextConfiguration != null ? contextConfiguration.newTaskContext(task) : null;
    }

    @Override
    public void run() {
        final Context previousContext = Utils.setContext(context);
        try {
            task.run();
        } finally {
            Utils.setContext(previousContext);
        }
    }

}
