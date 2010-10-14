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

import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.controller.DomainControllerClient;
import org.jboss.as.domain.controller.ModelUpdateResponse;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.UpdateFailedException;

/**
 * A client to integarte with a local domain controller instance.
 *
 * @author John Bailey
 */
public class LocalDomainControllerClient implements DomainControllerClient {
    private static final String ID = "LOCAL";
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
    public String getId() {
        return ID;
    }

    /** {@inheritDoc} */
    public void updateDomainModel(final DomainModel domain) {
        serverManager.setDomain(domain);
    }

    /** {@inheritDoc} */
    public List<ModelUpdateResponse<?>> updateHostModel(List<AbstractHostModelUpdate<?>> updates) {
        final List<ModelUpdateResponse<?>> responses = new ArrayList<ModelUpdateResponse<?>>(updates.size());
        for(AbstractHostModelUpdate<?> update : updates) {
            responses.add(executeUpdate(update));
        }
        return responses;
    }

    /** {@inheritDoc} */
    public List<ModelUpdateResponse<List<ServerIdentity>>> updateDomainModel(List<AbstractDomainModelUpdate<?>> updates) {
        final List<ModelUpdateResponse<List<ServerIdentity>>> responses = new ArrayList<ModelUpdateResponse<List<ServerIdentity>>>(updates.size());
        for(AbstractDomainModelUpdate<?> update : updates) {
            responses.add(executeUpdate(update));
        }
        return responses;
    }

    public List<ModelUpdateResponse<?>> updateServerModel(final List<AbstractServerModelUpdate<?>> updates, final String serverName) {
        List<ModelUpdateResponse<?>> responses = new ArrayList<ModelUpdateResponse<?>>();
        for (AbstractServerModelUpdate<?> update : updates) {
            responses.add(executeUpdate(update, serverName));
        }
        return responses;
    }

    private ModelUpdateResponse<List<ServerIdentity>> executeUpdate(AbstractDomainModelUpdate<?> domainUpdate) {
        final List<ServerIdentity> result = null;  // TODO execute update
        return new ModelUpdateResponse<List<ServerIdentity>>(result);
    }

    private <R> ModelUpdateResponse<R> executeUpdate(AbstractHostModelUpdate<R> hostUpdate) {
        final R result = null;  // TODO execute update
        return new ModelUpdateResponse<R>(result);
    }

    private <R> ModelUpdateResponse<R> executeUpdate(final AbstractServerModelUpdate<R> update, final String serverName) {
        Server server = serverManager.getServer(serverName);
        if (server == null) {
            // TODO better handle removal of server while client is concurrently
            // processing results
            return new ModelUpdateResponse<R>(new UpdateFailedException("unknown server " + serverName));
        }
        else {
            try {
                R result = server.applyUpdate(update);
                return new ModelUpdateResponse<R>(result);
            } catch (UpdateFailedException e) {
                return new ModelUpdateResponse<R>(e);
            }
        }

    }

    /** {@inheritDoc} */
    public boolean isActive() {
        return true;
    }
}
