/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.provider;

import java.util.Set;

import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistry;

/**
 * Simple {@link ServiceProviderRegistration} implementation that delegates {@link #getProviders()} back to the factory.
 * @author Paul Ferraro
 */
public class SimpleServiceProviderRegistration<T> implements ServiceProviderRegistration<T> {

    private final T service;
    private final ServiceProviderRegistry<T> registry;
    private final Runnable closeTask;

    public SimpleServiceProviderRegistration(T service, ServiceProviderRegistry<T> registry, Runnable closeTask) {
        this.service = service;
        this.registry = registry;
        this.closeTask = closeTask;
    }

    @Override
    public T getService() {
        return this.service;
    }

    @Override
    public Set<Node> getProviders() {
        return this.registry.getProviders(this.service);
    }

    @Override
    public void close() {
        this.closeTask.run();
    }
}
