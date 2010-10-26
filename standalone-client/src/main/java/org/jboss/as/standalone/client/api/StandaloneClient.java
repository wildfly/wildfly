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

package org.jboss.as.standalone.client.api;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.List;

import org.jboss.as.deployment.client.api.server.ServerDeploymentManager;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.as.standalone.client.impl.StandaloneClientImpl;

/**
 * Client interface used to interact with the standalone management infrastructure. This interface allows clients to get
 * information about the standalone server as well as apply updates to the server.
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 */
public interface StandaloneClient {

    /**
     * Get the current server model.
     *
     * @return the current server model
     */
    ServerModel getServerModel();

    /**
     * Apply a series of updates to the server.
     *
     * @param updates The server updates to apply
     * @return The results of the update
     */
    List<StandaloneUpdateResult<?>> applyUpdates(List<AbstractServerModelUpdate<?>> updates);

    /**
     * Add the content for a deployment to the server.
     *
     * @param name The deployment name
     * @param runtimeName The runtime name
     * @param stream The data stream for the deployment
     * @return The unique hash for the deployment
     */
    byte[] addDeploymentContent(String name, String runtimeName, InputStream stream);

    /**
     * Gets a {@link ServerDeploymentManager} that provides a convenience API
     * for manipulating deployments.
     *
     * @return the deployment manager. Will not be {@code null}
     */
    ServerDeploymentManager getDeploymentManager();

    /**
     * Factory used to create an {@link org.jboss.as.standalone.client.api.StandaloneClient} instance for a remote address
     * and port.
     */
    class Factory {
        /**
         * Create an {@link org.jboss.as.standalone.client.api.StandaloneClient} instance for a remote address and port.
         *
         * @param address The remote address to connect to
         * @param port The remote port
         * @return A domain client
         */
        public static StandaloneClient create(final InetAddress address, int port) {
            return new StandaloneClientImpl(address, port);
        }
    }
}
