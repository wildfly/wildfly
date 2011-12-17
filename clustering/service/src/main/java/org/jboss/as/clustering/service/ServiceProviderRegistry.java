/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.service;

import java.util.Set;

import org.jboss.as.clustering.ClusterNode;

/**
 * A registry of which nodes can provide a given service.
 * @author Paul Ferraro
 */
public interface ServiceProviderRegistry {

    interface Listener {
        /**
         * Indicates that the set of nodes providing a given service has changed.
         * @param nodes the new set of nodes providing the given service
         * @param merge indicates whether or not this provision change was the result of a network partition merge.
         */
        void serviceProvidersChanged(Set<ClusterNode> nodes, boolean merge);
    }

    /**
     * Registers the current node as a provider for the specified service.
     * @param service the name of the provided service.
     * @param listener the object to notify in the event the set of nodes providing the specified service changes.
     */
    void register(String service, Listener listener);

    /**
     * Unregisters the current node as a provider for the specified service.
     * @param service a service name.
     */
    void unregister(String service);

    /**
     * Returns the set of nodes that provide the specified service.
     * @param service a service name
     * @return a set of nodes.
     */
    Set<ClusterNode> getServiceProviders(String service);
}
