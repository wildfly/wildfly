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
import org.jboss.jca.core.workmanager.DistributedWorkManagerImpl;

/**
 * A named WorkManager.
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class NamedDistributedWorkManager extends DistributedWorkManagerImpl {

    private final boolean elytronEnabled;


    /**
     * Constructor
     * @param name The name of the WorkManager
     */
    public NamedDistributedWorkManager(String name, final boolean elytronEnabled) {
        super();
        setName(name);
        this.elytronEnabled = elytronEnabled;
    }
    protected WildflyWorkWrapper createWorKWrapper(SecurityIntegration securityIntegration, Work work,
                                                   ExecutionContext executionContext, WorkListener workListener, CountDownLatch startedLatch,
                                                   CountDownLatch completedLatch, long startTimeout) {
        return new WildflyWorkWrapper(this, securityIntegration, work, executionContext, workListener,
                startedLatch, completedLatch, System.currentTimeMillis(), startTimeout);
    }
    public boolean isElytronEnabled() {
        return elytronEnabled;
    }
}
