/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.provider;

import java.util.Set;

import org.wildfly.clustering.Registration;
import org.wildfly.clustering.group.Node;

/**
 * Registration of a provided service.
 *
 * @author Paul Ferraro
 * @param <T> a service type
 * @deprecated Replaced by {@link org.wildfly.clustering.server.provider.ServiceProviderRegistration}.
 */
@Deprecated(forRemoval = true)
public interface ServiceProviderRegistration<T> extends Registration {

    /**
     * Listener for service provider changes.
     */
    interface Listener {
        /**
         * Indicates that the set of nodes providing a given service has changed.
         *
         * @param nodes the new set of nodes providing the given service
         */
        void providersChanged(Set<Node> nodes);
    }

    /**
     * The provided service.
     *
     * @return a service identifier
     */
    T getService();

    /**
     * Returns the set of nodes that can provide this service.
     *
     * @return a set of nodes
     */
    Set<Node> getProviders();
}
