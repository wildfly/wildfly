/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller;

import java.util.List;
import java.util.Map;

import org.jboss.as.domain.client.api.HostUpdateResult;
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateResultHandlerResponse;

/**
 * Client interface via which a domain controller interacts with a server manager.
 * Each server manager is registered with the domain controller and provides the
 * domain controller an object implementing interface to communicate with the client.
 *
 *  @author John Bailey
 */
public interface ServerManagerClient {
    /**
     * Get the identifier for the Server Manager.
     *
     * @return the identifier. Cannot be <code>null</code>. Must match the
     *   {@link HostModel#getName() name property} of the Server Manager's
     *   {@link HostModel}.
     */
    String getId();

    /**
     * Check the server manager client to verify it is still active.
     *
     * @return true if the client is active, false if not.
     */
    boolean isActive();

    /**
     * Update the client with a new version of the full domain.
     *
     * @param domain The domain configuration
     */
    void updateDomainModel(DomainModel domain);

    /**
     * Update the client with a list of domain model updates.
     *
     * @param updates The updates to process
     * @return A list of response objects to reflect the result of each update executed.
     */
    List<ModelUpdateResponse<List<ServerIdentity>>> updateDomainModel(List<AbstractDomainModelUpdate<?>> updates);

    /**
     * Get the current state of the server manager's {@link HostModel}.
     *
     * @return the host model. Will not return <code>null</code>
     */
    HostModel getHostModel();

    /**
     * Update the client with a list of host model updates.
     *
     * @param updates The updates to process
     * @return A list of response objects to reflect the result of each update executed.
     */
    List<HostUpdateResult<?>> updateHostModel(List<AbstractHostModelUpdate<?>> updates);

    /**
     * Gets a list of all servers known to the server manager, along with their current
     * {@link ServerStatus status}.
     *
     * @return the servers and their current status. Will not be <code>null</code>
     */
    Map<ServerIdentity, ServerStatus> getServerStatuses();

    /**
     * Gets the current running configuration for the given server.
     *
     * @param serverName the name of the server. Cannot be <code>null</code>
     * @return the server model, or <code>null</code> if the server is unknown or not running
     */
    ServerModel getServerModel(String serverName);

    /**
     * Update the given server with a server model update.
     *
     * @param serverName the name of the server to which the update should be applied.
     *                   Cannot be <code>null</code>
     * @param update the updates. Cannot be <code>null</code>
     * @param allowOverallRollback <code>true</code> if the failure to apply an update
     *              should result in previously successfully applied updates being
     *              rolled back
     *
     * @return list of response objects reflecting the result of each update executed.
     */
    List<UpdateResultHandlerResponse<?>> updateServerModel(final String serverName,
            final List<AbstractServerModelUpdate<?>> updates, final boolean allowOverallRollback);

    /**
     * Start the given server.
     *
     * @param serverName the name of the server that should be started.
     *                   Cannot be <code>null</code>
     *
     * @return the status of the server following the start attempt
     */
    ServerStatus startServer(String serverName);

    /**
     * Stop the given server.
     *
     * @param serverName the name of the server that should be stopped.
     *                   Cannot be <code>null</code>
     * @param gracefulTimeout maximum number of milliseconds the server should
     *                        wait for graceful shutdown, or {@code -1} if
     *                        graceful shutdown is not required
     *
     * @return the status of the server following the stop attempt
     */
    ServerStatus stopServer(String serverName, long gracefulTimeout);

    /**
     * Restart the given server.
     *
     * @param serverName the name of the server that should be restarted.
     *                   Cannot be <code>null</code>
     * @param gracefulTimeout maximum number of milliseconds the server should
     *                        wait for graceful shutdown, or {@code -1} if
     *                        graceful shutdown is not required
     *
     * @return the status of the server following the start attempt
     */
    ServerStatus restartServer(String serverName, long gracefulTimeout);
}
