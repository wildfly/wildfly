/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager;

import jakarta.resource.spi.work.ExecutionContext;
import jakarta.resource.spi.work.Work;
import jakarta.resource.spi.work.WorkListener;
import java.util.concurrent.CountDownLatch;

import org.jboss.jca.core.spi.security.SecurityIntegration;
import org.jboss.jca.core.workmanager.WorkManagerImpl;

/**
 * A named WorkManager.
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author Flavia Rainone
 */
public class NamedWorkManager extends WorkManagerImpl {

    /** Default WorkManager name */
    public static final String DEFAULT_NAME = "default";

    /**
     * Constructor
     * @param name The name of the WorkManager
     */
    public NamedWorkManager(String name) {
        super();
        setName(name);
    }

    @Override
    protected WildflyWorkWrapper createWorkWrapper(SecurityIntegration securityIntegration, Work work,
                                            ExecutionContext executionContext, WorkListener workListener, CountDownLatch startedLatch,
                                            CountDownLatch completedLatch, long creationTime, long startTimeout) {
        return new WildflyWorkWrapper(this, securityIntegration, work, executionContext, workListener,
                startedLatch, completedLatch, creationTime, startTimeout);
    }
}
