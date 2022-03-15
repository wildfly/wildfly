/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import org.wildfly.clustering.Registration;
import org.wildfly.clustering.group.Node;

/**
 * Registration of a provided service.
 *
 * @author Paul Ferraro
 * @param <T> a service type
 */
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
