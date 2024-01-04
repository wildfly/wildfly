/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.provider;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistration.Listener;

/**
 * Factory that provides a non-clustered {@link ServiceProviderRegistrationFactory} implementation.
 * @author Paul Ferraro
 */
public class LocalServiceProviderRegistry<T> implements AutoCloseableServiceProviderRegistry<T> {

    private final Set<T> services = ConcurrentHashMap.newKeySet();
    private final Group group;

    public LocalServiceProviderRegistry(Group group) {
        this.group = group;
    }

    @Override
    public Group getGroup() {
        return this.group;
    }

    @Override
    public ServiceProviderRegistration<T> register(T service) {
        this.services.add(service);
        return new SimpleServiceProviderRegistration<>(service, this, () -> this.services.remove(service));
    }

    @Override
    public ServiceProviderRegistration<T> register(T service, Listener listener) {
        return this.register(service);
    }

    @Override
    public Set<Node> getProviders(T service) {
        return this.services.contains(service) ? Collections.singleton(this.getGroup().getLocalMember()) : Collections.emptySet();
    }

    @Override
    public Set<T> getServices() {
        return Collections.unmodifiableSet(this.services);
    }

    @Override
    public void close() {
        // Do nothing
    }
}
