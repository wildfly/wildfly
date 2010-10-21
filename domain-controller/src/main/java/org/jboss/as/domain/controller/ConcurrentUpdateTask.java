package org.jboss.as.domain.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/** A task that uses the executor service to concurrently execute other tasks */
class ConcurrentUpdateTask implements Runnable {

    private final List<Runnable> concurrentTasks;
    private final ExecutorService executorService;

    ConcurrentUpdateTask(final List<Runnable> concurrentTasks, final ExecutorService executorService) {
        this.concurrentTasks = concurrentTasks;
        this.executorService = executorService;
    }

    @Override
    public void run() {

        // Submit each task to the executor
        List<Future<?>> futures = new ArrayList<Future<?>>();
        for (Runnable r : concurrentTasks) {
            futures.add(executorService.submit(r));
        }

        // Wait until all complete before returning
        for (int i = 0; i < futures.size(); i++) {
            Future<?> future = futures.get(i);
            try {
                future.get();
            } catch (InterruptedException e) {
                DomainDeploymentHandler.logger.errorf("ConcurrentUpdateTask caught InterruptedException waiting for task %s; returning", concurrentTasks.get(i).toString());
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException e) {
                DomainDeploymentHandler.logger.errorf(e, "ConcurrentUpdateTask caught ExecutionException waiting for task %s", concurrentTasks.get(i).toString());
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ConcurrentUpdateTask{tasks={");
        for (int i = 0; i < concurrentTasks.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(concurrentTasks.get(i).toString());
        }
        sb.append("}}");
        return sb.toString();
    }
}
