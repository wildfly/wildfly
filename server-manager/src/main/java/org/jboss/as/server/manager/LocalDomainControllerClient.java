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
import org.jboss.as.domain.controller.DomainControllerClient;
import org.jboss.as.domain.controller.ModelUpdateResponse;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.DomainModel;

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
    public List<ModelUpdateResponse<?>> updateDomainModel(List<AbstractDomainModelUpdate<?>> updates) {
        final List<ModelUpdateResponse<?>> responses = new ArrayList<ModelUpdateResponse<?>>(updates.size());
        for(AbstractDomainModelUpdate<?> update : updates) {
            responses.add(executeUpdate(update));
        }
        return responses;
    }

    private <R> ModelUpdateResponse<R> executeUpdate(AbstractDomainModelUpdate<R> domainUpdate) {
        final R result = null;  // TODO execute update
        return new ModelUpdateResponse<R>(result);
    }

    private <R> ModelUpdateResponse<R> executeUpdate(AbstractHostModelUpdate<R> hostUpdate) {
        final R result = null;  // TODO execute update
        return new ModelUpdateResponse<R>(result);
    }

    /** {@inheritDoc} */
    public boolean isActive() {
        return true;
    }
}
