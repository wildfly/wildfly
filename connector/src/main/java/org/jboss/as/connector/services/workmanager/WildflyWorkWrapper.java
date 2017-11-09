/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.connector.services.workmanager;

import java.util.concurrent.CountDownLatch;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkListener;

import org.jboss.as.connector.security.CallbackImpl;
import org.jboss.as.connector.security.ElytronSecurityContext;
import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.jca.core.spi.security.SecurityIntegration;
import org.jboss.jca.core.workmanager.WorkManagerImpl;

/**
 * Extension of WildflyWorkWrapper with added Elytron support.
 *
 * @author Flavia Rainone
 */
public class WildflyWorkWrapper extends org.jboss.jca.core.workmanager.WorkWrapper {
    /**
     * Create a new WildflyWorkWrapper
     *
     * @param workManager      the work manager
     * @param si               The security integration
     * @param work             the work
     * @param executionContext the execution context
     * @param workListener     the WorkListener
     * @param startedLatch     The latch for when work has started
     * @param completedLatch   The latch for when work has completed
     * @param startTime        The start time
     * @throws IllegalArgumentException for null work, execution context or a negative start timeout
     */
    WildflyWorkWrapper(WorkManagerImpl workManager, SecurityIntegration si, Work work, ExecutionContext executionContext,
            WorkListener workListener, CountDownLatch startedLatch, CountDownLatch completedLatch, long startTime) {
        super(workManager, si, work, executionContext, workListener, startedLatch, completedLatch, startTime);
    }

    @Override
    protected void runWork() throws WorkCompletedException {
        // if there is security and elytron is enabled, we need to let the context run the remainder of the work
        // so the context can run the work as the specified Elytron identity
        if (securityIntegration.getSecurityContext() != null &&
                ((CallbackImpl) workManager.getCallbackSecurity()).isElytronEnabled())
            ((ElytronSecurityContext) securityIntegration.getSecurityContext()).runWork(() -> {
                try {
                    WildflyWorkWrapper.super.runWork();
                } catch (WorkCompletedException e) {
                    ConnectorLogger.ROOT_LOGGER.unexceptedWorkerCompletionError(e.getLocalizedMessage(),e);
                }
            });
        // delegate to super class if there is no elytron enabled
        else super.runWork();
    }
}
