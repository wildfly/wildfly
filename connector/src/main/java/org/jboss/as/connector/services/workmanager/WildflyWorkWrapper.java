/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.services.workmanager;

import java.util.concurrent.CountDownLatch;

import jakarta.resource.spi.work.ExecutionContext;
import jakarta.resource.spi.work.Work;
import jakarta.resource.spi.work.WorkCompletedException;
import jakarta.resource.spi.work.WorkListener;

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
            WorkListener workListener, CountDownLatch startedLatch, CountDownLatch completedLatch, long creationTime, long startTimeout) {
        super(workManager, si, work, executionContext, workListener, startedLatch, completedLatch, creationTime, startTimeout);
    }

    @Override
    protected void runWork() throws WorkCompletedException {
        if (securityIntegration.getSecurityContext() != null)
            ((ElytronSecurityContext) securityIntegration.getSecurityContext()).runWork(() -> {
                try {
                    WildflyWorkWrapper.super.runWork();
                } catch (WorkCompletedException e) {
                    ConnectorLogger.ROOT_LOGGER.unexceptedWorkerCompletionError(e.getLocalizedMessage(),e);
                }
            });
        else super.runWork();
    }
}
