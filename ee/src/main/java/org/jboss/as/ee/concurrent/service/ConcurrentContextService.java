/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.service;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.concurrent.ConcurrentContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A service holding a concurrent context.
 * @author Eduardo Martins
 */
public class ConcurrentContextService implements Service<ConcurrentContext> {

    private final ConcurrentContext concurrentContext;
    private volatile boolean started;

    public ConcurrentContextService(ConcurrentContext concurrentContext) {
        this.concurrentContext = concurrentContext;
    }

    @Override
    public void start(StartContext context) throws StartException {
        started = true;
        concurrentContext.setServiceName(context.getController().getName());
    }

    @Override
    public void stop(StopContext context) {
        started = false;
    }

    @Override
    public ConcurrentContext getValue() throws IllegalStateException, IllegalArgumentException {
        if(!started) {
            throw EeLogger.ROOT_LOGGER.serviceNotStarted();
        }
        return concurrentContext;
    }
}
