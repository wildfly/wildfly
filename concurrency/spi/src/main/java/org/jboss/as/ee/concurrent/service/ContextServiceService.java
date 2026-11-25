/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent.service;

import org.jboss.as.ee.concurrent.ConcurrencyImplementation;
import org.jboss.as.ee.concurrent.ContextServiceTypesConfiguration;
import org.jboss.as.ee.concurrent.WildFlyContextService;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * Service responsible for managing a context service impl's lifecycle.
 *
 * @author Eduardo Martins
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ContextServiceService extends EEConcurrentAbstractService<WildFlyContextService> {

    private final String name;
    private final ContextServiceTypesConfiguration contextServiceTypesConfiguration;
    private volatile WildFlyContextService contextService;

    public ContextServiceService(final String name, final String jndiName, final ContextServiceTypesConfiguration contextServiceTypesConfiguration) {
        super(jndiName);
        this.name = name;
        this.contextServiceTypesConfiguration = contextServiceTypesConfiguration;
    }

    @Override
    void startValue(final StartContext context) {
        contextService = ConcurrencyImplementation.INSTANCE.newContextService(name, contextServiceTypesConfiguration);
    }

    @Override
    void stopValue(final StopContext context) {
        contextService = null;
    }

    public WildFlyContextService getValue() throws IllegalStateException {
        final WildFlyContextService value = this.contextService;
        if (value == null) {
            throw EeLogger.ROOT_LOGGER.concurrentServiceValueUninitialized();
        }
        return value;
    }

}
