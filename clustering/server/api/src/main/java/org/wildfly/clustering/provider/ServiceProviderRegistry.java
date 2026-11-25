/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.provider;

import java.util.Set;

import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;

/**
 * A distributed registry of service providers.
 * Allows capability to query which nodes can provide a given service.
 * @author Paul Ferraro
 * @param <T> a service type
 * @deprecated Replaced by {@link org.wildfly.clustering.server.provider.ServiceProviderRegistry}.
 */
@Deprecated(forRemoval = true)
public interface ServiceProviderRegistry<T> extends Registrar<T> {

    /**
     * Returns the group with which to register service providers.
     *
     * @return a group
     */
    Group getGroup();

    /**
     * Registers the local node as providing the specified service.
     *
     * @param service a service to register
     * @return a new service provider registration
     */
    @Override
    ServiceProviderRegistration<T> register(T service);

    /**
     * Registers the local node as providing the specified service, using the specified listener.
     *
     * @param service a service to register
     * @param listener a registry listener
     * @return a new service provider registration
     */
    ServiceProviderRegistration<T> register(T service, ServiceProviderRegistration.Listener listener);

    /**
     * Returns the set of nodes that can provide the specified service.
     *
     * @param service a service to obtain providers for
     * @return a set of nodes
     */
    Set<Node> getProviders(T service);

    /**
     * Returns the complete list of services known to this registry.
     * @return a set of services
     */
    Set<T> getServices();
}
