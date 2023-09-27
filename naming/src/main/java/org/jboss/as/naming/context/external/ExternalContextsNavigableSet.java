/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming.context.external;

import org.jboss.msc.service.ServiceName;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * An {@link ExternalContexts} implementation using a {@link java.util.NavigableSet} to store the service names of the existent external contexts.
 * @author Eduardo Martins
 */
public class ExternalContextsNavigableSet implements ExternalContexts {

    /**
     *
     */
    private final ConcurrentSkipListSet<ServiceName> externalContexts;

    /**
     *
     */
    public ExternalContextsNavigableSet() {
        externalContexts = new ConcurrentSkipListSet<>();
    }

    @Override
    public void addExternalContext(ServiceName serviceName) {
        externalContexts.add(serviceName);
    }

    @Override
    public boolean removeExternalContext(ServiceName serviceName) {
        return externalContexts.remove(serviceName);
    }

    @Override
    public ServiceName getParentExternalContext(ServiceName serviceName) {
        final ServiceName lower = externalContexts.lower(serviceName);
        return lower != null && lower.isParentOf(serviceName) ? lower : null;
    }
}
