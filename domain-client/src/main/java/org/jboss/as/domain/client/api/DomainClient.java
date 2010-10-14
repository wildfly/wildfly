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

package org.jboss.as.domain.client.api;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.List;
import org.jboss.as.deployment.client.api.domain.DomainDeploymentManager;
import org.jboss.as.domain.client.impl.DomainClientImpl;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.DomainModel;

/**
 * Client interface used to interact with the domain management infrastructure.  THis interface allows clients to get
 * information about the domain as well as apply updates to the domain.
 *
 * @author John Bailey
 */
public interface DomainClient {

    /**
     * Get the current domain model.
     *
     * @return The domain model
     */
    DomainModel getDomainModel();

    /**
     * Apply a series of updates to the domain.
     *
     * @param updates The domain updates to apply
     * @return The results of the update
     */
    List<DomainUpdateResult<?>> applyUpdates(List<AbstractDomainModelUpdate<?>> updates);

    /**
     * Apply an update to the domain, using the given {@link DomainUpdateApplier}
     * to control the update process.
     *
     * @param <R> the type of result that is returned by this update type
     * @param <P> the type of the parameter to pass to the handler instance
     * @param update the update. Cannot be <code>null</code>
     * @param updateApplier the update applier. Cannot be <code>null</code>
     * @param param the parameter to pass to the handler
     */
    <R, P> void applyUpdate(AbstractDomainModelUpdate<R> update, DomainUpdateApplier<R, P> updateApplier, P param);

    /**
     * Add the content for a deployment to the domain controller.
     *
     * @param name The deployment name
     * @param runtimeName The runtime name
     * @param stream The data stream for the deployment
     * @return The unique hash for the deployment
     */
    byte[] addDeploymentContent(String name, String runtimeName, InputStream stream);

    /**
     * Gets a {@link DomainDeploymentManager} that provides a convenience API
     * for manipulating domain deployments.
     *
     * @return the deployment manager. Will not be {@code null}
     */
    DomainDeploymentManager getDeploymentManager();

    /**
     * Factory used to create an {@link org.jboss.as.domain.client.api.DomainClient} instance for a remote address
     * and port.
     */
    class Factory {
        /**
         * Create an {@link org.jboss.as.domain.client.api.DomainClient} instance for a remote address and port.
         *
         * @param address The remote address to connect to
         * @param port The remote port
         * @return A domain client
         */
        DomainClient create(final InetAddress address, int port) {
            return new DomainClientImpl(address, port);
        }
    }
}
