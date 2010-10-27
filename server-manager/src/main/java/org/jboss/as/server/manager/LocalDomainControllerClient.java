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

package org.jboss.as.server.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.as.domain.client.api.HostUpdateResult;
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.controller.ServerManagerClient;
import org.jboss.as.domain.controller.ModelUpdateResponse;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandlerResponse;

/**
 * A client to integrate with a local domain controller instance.
 *
 * @author John Bailey
 */
public class LocalDomainControllerClient implements ServerManagerClient {

    private final ServerManager serverManager;

    /**
     * Create an instance with a server manger.
     *
     * @param serverManager The local server manager instance.
     */
    public LocalDomainControllerClient(final ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return serverManager.getName();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isActive() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void updateDomainModel(final DomainModel domain) {
        serverManager.setDomain(domain);
    }

    @Override
    public HostModel getHostModel() {
        return serverManager.getHostModel();
    }

    /** {@inheritDoc} */
    @Override
    public List<HostUpdateResult<?>> updateHostModel(List<AbstractHostModelUpdate<?>> updates) {
        return serverManager.applyHostUpdates(updates);
    }

    /** {@inheritDoc} */
    @Override
    public List<ModelUpdateResponse<List<ServerIdentity>>> updateDomainModel(List<AbstractDomainModelUpdate<?>> updates) {
        final List<ModelUpdateResponse<List<ServerIdentity>>> responses = new ArrayList<ModelUpdateResponse<List<ServerIdentity>>>(updates.size());
        for(AbstractDomainModelUpdate<?> update : updates) {
            ModelUpdateResponse<List<ServerIdentity>> response = executeUpdate(update);
            responses.add(response);
            if (!response.isSuccess())
                break;
        }
        return responses;
    }

    @Override
    public Map<ServerIdentity, ServerStatus> getServerStatuses() {
        return serverManager.getServerStatuses();
    }

    @Override
    public ServerModel getServerModel(String serverName) {
        return serverManager.getServerModel(serverName);
    }

    @Override
    public List<UpdateResultHandlerResponse<?>> updateServerModel(final String serverName, final List<AbstractServerModelUpdate<?>> updates, final boolean allowOverallRollback) {
        return serverManager.applyServerUpdates(serverName, updates, allowOverallRollback);
    }

    @Override
    public ServerStatus restartServer(String serverName, long gracefulTimeout) {
        return serverManager.restartServer(serverName, gracefulTimeout);
    }

    @Override
    public ServerStatus startServer(String serverName) {
        return serverManager.startServer(serverName);
    }

    @Override
    public ServerStatus stopServer(String serverName, long gracefulTimeout) {
        return serverManager.stopServer(serverName, gracefulTimeout);
    }

    private ModelUpdateResponse<List<ServerIdentity>> executeUpdate(AbstractDomainModelUpdate<?> domainUpdate) {
        try {
            final List<ServerIdentity> result = serverManager.getModelManager().applyDomainModelUpdate(domainUpdate, false);
            return new ModelUpdateResponse<List<ServerIdentity>>(result);
        } catch (UpdateFailedException e) {
            return new ModelUpdateResponse<List<ServerIdentity>>(e);
        }
    }

    // TODO use executeUpdate(AbstractHostModelUpdate<?> hostUpdate) with a "HostUpdateApplier" API
    private ModelUpdateResponse<List<ServerIdentity>> executeUpdate(AbstractHostModelUpdate<?> hostUpdate) {
        try {
            final List<ServerIdentity> result = serverManager.getModelManager().applyHostModelUpdate(hostUpdate);
            return new ModelUpdateResponse<List<ServerIdentity>>(result);
        } catch (UpdateFailedException e) {
            return new ModelUpdateResponse<List<ServerIdentity>>(e);
        }
    }
}
