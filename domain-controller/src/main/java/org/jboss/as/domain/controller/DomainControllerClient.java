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

import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;

/**
 * Contract for a domain controller client.  Each client is registered with the domain controller and provides the
 * domain controller an interface to communicate with the client.
 *
 *  @author John Bailey
 */
public interface DomainControllerClient {
    /**
     * Get the identifier for the client.
     *
     * @return The identifier
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
     * Update the client with a list of host model updates.
     *
     * @param updates The updates to process
     * @return A list of response objects to reflect the result of each update executed.
     */
    List<ModelUpdateResponse<?>> updateHostModel(List<AbstractHostModelUpdate<?>> updates);

    /**
     * Update the client with a list of domain model updates.
     *
     * @param updates The updates to process
     * @return A list of response objects to reflect the result of each update executed.
     */
    List<ModelUpdateResponse<List<ServerIdentity>>> updateDomainModel(List<AbstractDomainModelUpdate<?>> updates);

    /**
     * Update the given server with a server model update.
     *
     * @param update the update. Cannot be <code>null</code>
     * @param serverName the name of the server to which the update should be applied.
     *                   Cannot be <code>null</code>
     *
     * @return list of response objects reflecting the result of each update executed.
     */
    List<ModelUpdateResponse<?>> updateServerModel(final List<AbstractServerModelUpdate<?>> update, final String serverName);
}
