/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.jboss.as.ee.concurrent.ManagedThreadFactoryImpl;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Eduardo Martins
 */
public class ManagedThreadFactoryService extends EEConcurrentAbstractService<ManagedThreadFactoryImpl> {

    private volatile ManagedThreadFactoryImpl managedThreadFactory;
    private final Consumer<ManagedThreadFactoryImpl> consumer;
    private final String name;
    private final DelegatingSupplier<ContextServiceImpl> contextServiceSupplier = new DelegatingSupplier<>();
    private final int priority;

    /**
     * @param name
     * @param jndiName
     * @param priority
     * @see org.jboss.as.ee.concurrent.ManagedThreadFactoryImpl#ManagedThreadFactoryImpl(String, org.glassfish.enterprise.concurrent.ContextServiceImpl, int)
     */
    public ManagedThreadFactoryService(final Consumer<ManagedThreadFactoryImpl> consumer, final Supplier<ContextServiceImpl> ctxServiceSupplier, String name, String jndiName, int priority) {
        super(jndiName);
        this.consumer = consumer;
        this.name = name;
        this.contextServiceSupplier.set(ctxServiceSupplier);
        this.priority = priority;
    }

    @Override
    void startValue(StartContext context) throws StartException {
        final String threadFactoryName = "EE-ManagedThreadFactory-"+name;
        consumer.accept(managedThreadFactory = new ManagedThreadFactoryImpl(threadFactoryName, contextServiceSupplier.get(), priority));
    }

    @Override
    void stopValue(StopContext context) {
        managedThreadFactory.stop();
        consumer.accept(managedThreadFactory = null);
    }

    public ManagedThreadFactoryImpl getValue() throws IllegalStateException {
        if (this.managedThreadFactory == null) {
            throw EeLogger.ROOT_LOGGER.concurrentServiceValueUninitialized();
        }
        return managedThreadFactory;
    }

    public DelegatingSupplier<ContextServiceImpl> getContextServiceSupplier() {
        return contextServiceSupplier;
    }
}
