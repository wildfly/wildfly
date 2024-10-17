/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

/**
 * Runtime stats from an executor.
 * @author emmartins
 */
public interface ManagedExecutorRuntimeStats {

    /**
     *
     * @return the approximate number of threads that are actively executing tasks
     */
    int getActiveThreadsCount();

    /**
     *
     * @return the approximate total number of tasks that have completed execution
     */
    long getCompletedTaskCount();

    /**
     *
     * @return the number of executor threads that are hung
     */
    int getHungThreadsCount();

    /**
     *
     * @return the largest number of executor threads
     */
    int getMaxThreadsCount();

    /**
     *
     * @return the current size of the executor's task queue
     */
    int getQueueSize();

    /**
     *
     * @return the approximate total number of tasks that have ever been submitted for execution
     */
    long getTaskCount();

    /**
     *
     * @return the current number of executor threads
     */
    int getThreadsCount();
}
