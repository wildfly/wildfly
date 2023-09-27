/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.resourceadapters;

import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for exposing a {@link ManagedReferenceFactory} for a connection factory
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class ConnectionFactoryReferenceFactoryService implements Service<ManagedReferenceFactory>, ContextListAndJndiViewManagedReferenceFactory {
    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("connection-factory").append(
            "reference-factory");

    private final InjectedValue<Object> connectionFactoryValue = new InjectedValue<Object>();
    private ManagedReference reference;
    private final String name;

    public ConnectionFactoryReferenceFactoryService(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public synchronized void start(StartContext startContext) throws StartException {
        reference = new ValueManagedReference(connectionFactoryValue.getValue());
    }

    public synchronized void stop(StopContext stopContext) {
        reference = null;
    }

    public synchronized ManagedReferenceFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public synchronized ManagedReference getReference() {
        return reference;
    }

    public Injector<Object> getConnectionFactoryInjector() {
        return connectionFactoryValue;
    }

    @Override
    public String getInstanceClassName() {
        final Object value = reference != null ? reference.getInstance() : null;
        return value != null ? value.getClass().getName() : ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
    }

    @Override
    public String getJndiViewInstanceValue() {
        final Object value = reference != null ? reference.getInstance() : null;
        return String.valueOf(value);
    }
}
