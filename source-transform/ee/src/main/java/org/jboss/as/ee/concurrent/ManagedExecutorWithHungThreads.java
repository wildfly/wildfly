/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020 Red Hat, Inc., and individual contributors
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

import org.glassfish.enterprise.concurrent.AbstractManagedThread;
import org.jboss.as.ee.logging.EeLogger;

import java.util.Collection;

/**
 * A managed executor with support for hung threads.
 * @author emmartins
 */
public interface ManagedExecutorWithHungThreads {

    /**
     *
     * @return the executor's name
     */
    String getName();

    /**
     * Attempts to terminate the executor's hung tasks, by cancelling such tasks.
     * @return the number of hung tasks cancelled
     */
    default void terminateHungTasks() {
        final String executorName = getClass().getSimpleName() + ":" + getName();
        EeLogger.ROOT_LOGGER.debugf("Cancelling %s hung tasks...", executorName);
        final Collection<AbstractManagedThread> hungThreads = getHungThreads();
        if (hungThreads != null) {
            for (AbstractManagedThread t : hungThreads) {
                final String taskIdentityName = t.getTaskIdentityName();
                try {
                    if (t instanceof ManagedThreadFactoryImpl.ManagedThread) {
                        if (((ManagedThreadFactoryImpl.ManagedThread)t).cancelTask()) {
                            EeLogger.ROOT_LOGGER.hungTaskCancelled(executorName, taskIdentityName);
                        } else {
                            EeLogger.ROOT_LOGGER.hungTaskNotCancelled(executorName, taskIdentityName);
                        }
                    }
                } catch (Throwable throwable) {
                    EeLogger.ROOT_LOGGER.huntTaskTerminationFailure(throwable, executorName, taskIdentityName);
                }
            }
        }
    }

    /**
     *
     * @return the executor's hung threads
     */
    Collection<AbstractManagedThread> getHungThreads();
}
