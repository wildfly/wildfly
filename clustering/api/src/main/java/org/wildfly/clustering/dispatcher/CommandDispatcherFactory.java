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
package org.wildfly.clustering.dispatcher;

import java.util.List;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.Node;

/**
 * Factory for creating a command dispatcher.
 * @author Paul Ferraro
 */
public interface CommandDispatcherFactory {
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("clustering", "dispatcher");

    /**
     * Creates a new command dispatcher for the specified service.
     * @param service a service name
     * @param context the context used for executing commands
     * @return a new command dispatcher
     */
    <C> CommandDispatcher<C> createCommandDispatcher(ServiceName service, C context);

    /**
     * Creates a new command dispatcher for the specified service.
     * @param service a service name
     * @param context the context used for executing commands
     * @param listener a listener for changes to cluster membership
     * @return a new command dispatcher
     */
    <C> CommandDispatcher<C> createCommandDispatcher(ServiceName service, C context, MembershipListener listener);

    /**
     * Indicates whether or not we are the group coordinator.
     * @return true, if we are the group coordinator, false otherwise.
     */
    boolean isCoordinator();

    /**
     * Returns the local node.
     * @return the local node.
     */
    Node getLocalNode();

    /**
     * Returns the group coordinator.
     * @return the group coordinator.
     */
    Node getCoordinatorNode();

    /**
     * Returns the list of nodes in this cluster.
     * @return a list of nodes
     */
    List<Node> getNodes();
}
