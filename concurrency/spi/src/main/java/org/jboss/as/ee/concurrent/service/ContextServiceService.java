/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent.service;

import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;
import org.jboss.as.ee.concurrent.ContextServiceTypesConfiguration;
import org.jboss.as.ee.concurrent.ContextServiceImpl;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * Service responsible for managing a context service impl's lifecycle.
 *
 * @author Eduardo Martins
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ContextServiceService extends EEConcurrentAbstractService<ContextServiceImpl> {

    private final String name;
    private final ContextSetupProvider contextSetupProvider;
    private final ContextServiceTypesConfiguration contextServiceTypesConfiguration;
    private volatile ContextServiceImpl contextService;

    public ContextServiceService(final String name, final String jndiName, final ContextSetupProvider contextSetupProvider, final ContextServiceTypesConfiguration contextServiceTypesConfiguration) {
        super(jndiName);
        this.name = name;
        this.contextSetupProvider = contextSetupProvider;
        this.contextServiceTypesConfiguration = contextServiceTypesConfiguration;
    }

    @Override
    void startValue(final StartContext context) {
        contextService = new ContextServiceImpl(name, contextSetupProvider, contextServiceTypesConfiguration);
    }

    @Override
    void stopValue(final StopContext context) {
        contextService = null;
    }

    public ContextServiceImpl getValue() throws IllegalStateException {
        final ContextServiceImpl value = this.contextService;
        if (value == null) {
            throw EeLogger.ROOT_LOGGER.concurrentServiceValueUninitialized();
        }
        return value;
    }

}
