/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming.service;

import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.naming.context.external.ExternalContexts;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A service containing the subsystem's {@link ExternalContexts}.
 * @author Eduardo Martins
 */
public class ExternalContextsService implements Service<ExternalContexts> {

    public static final ServiceName SERVICE_NAME = ContextNames.NAMING.append("externalcontexts");

    private final ExternalContexts externalContexts;
    private volatile boolean started = false;

    public ExternalContextsService(ExternalContexts externalContexts) {
        this.externalContexts = externalContexts;
    }

    @Override
    public void start(StartContext context) throws StartException {
        started = true;
    }

    @Override
    public void stop(StopContext context) {
        started = false;
    }

    @Override
    public ExternalContexts getValue() throws IllegalStateException, IllegalArgumentException {
        if(!started) {
            throw NamingLogger.ROOT_LOGGER.serviceNotStarted(SERVICE_NAME);
        }
        return externalContexts;
    }
}
