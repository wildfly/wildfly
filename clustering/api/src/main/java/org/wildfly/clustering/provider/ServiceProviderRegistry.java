/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
 */
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
