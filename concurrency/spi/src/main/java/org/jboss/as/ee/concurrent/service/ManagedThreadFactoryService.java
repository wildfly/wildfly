/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.ee.concurrent.ConcurrencyImplementation;
import org.jboss.as.ee.concurrent.WildFlyManagedThreadFactory;
import org.jboss.as.ee.concurrent.WildFlyContextService;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Eduardo Martins
 */
public class ManagedThreadFactoryService extends EEConcurrentAbstractService<WildFlyManagedThreadFactory> {

    private volatile WildFlyManagedThreadFactory managedThreadFactory;
    private final Consumer<WildFlyManagedThreadFactory> consumer;
    private final String name;
    private final DelegatingSupplier<WildFlyContextService> contextServiceSupplier = new DelegatingSupplier<>();
    private final int priority;

    /**
     * @param name
     * @param jndiName
     * @param priority
     */
    public ManagedThreadFactoryService(final Consumer<WildFlyManagedThreadFactory> consumer, final Supplier<WildFlyContextService> ctxServiceSupplier, String name, String jndiName, int priority) {
        super(jndiName);
        this.consumer = consumer;
        this.name = name;
        this.contextServiceSupplier.set(ctxServiceSupplier);
        this.priority = priority;
    }

    @Override
    void startValue(StartContext context) throws StartException {
        final String threadFactoryName = "EE-ManagedThreadFactory-"+name;
        consumer.accept(managedThreadFactory = ConcurrencyImplementation.INSTANCE.newManagedThreadFactory(threadFactoryName, contextServiceSupplier.get(), priority));
    }

    @Override
    void stopValue(StopContext context) {
        managedThreadFactory.stop();
        consumer.accept(managedThreadFactory = null);
    }

    public WildFlyManagedThreadFactory getValue() throws IllegalStateException {
        if (this.managedThreadFactory == null) {
            throw EeLogger.ROOT_LOGGER.concurrentServiceValueUninitialized();
        }
        return managedThreadFactory;
    }

    public DelegatingSupplier<WildFlyContextService> getContextServiceSupplier() {
        return contextServiceSupplier;
    }
}
