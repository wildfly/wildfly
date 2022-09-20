/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
