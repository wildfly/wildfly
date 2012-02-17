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

package org.jboss.as.controller.client.helpers.domain;

import javax.security.auth.callback.CallbackHandler;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.impl.DomainClientImpl;

/**
 * Client interface used to interact with the domain management infrastructure.  THis interface allows clients to get
 * information about the domain as well as apply updates to the domain.
 *
 * @author John Bailey
 */
public interface DomainClient extends ModelControllerClient {

    /**
     * Gets the list of currently running host controllers.
     *
     * @return the names of the host controllers. Will not be <code>null</code>
     */
    List<String> getHostControllerNames();

    /**
     * Add the content for a deployment to the domain controller. Note that
     * {@link #getDeploymentManager() the DomainDeploymentManager offers a
     * more convenient API for manipulating domain deployments.
     *
     * @param stream the data stream for the deployment
     * @return the unique hash for the deployment
     */
    byte[] addDeploymentContent(InputStream stream);

    /**
     * Gets a {@link DomainDeploymentManager} that provides a convenience API
     * for manipulating domain deployments.
     *
     * @return the deployment manager. Will not be {@code null}
     */
    DomainDeploymentManager getDeploymentManager();

    /**
     * Gets a list of all servers known to the domain, along with their current
     * {@link ServerStatus status}. Servers associated with host controllers that
     * are currently off line will not be included.
     *
     * @return the servers and their current status. Will not be <code>null</code>
     */
    Map<ServerIdentity, ServerStatus> getServerStatuses();

    /**
     * Starts the given server. Ignored if the server is not stopped.
     *
     * @param hostControllerName the name of the host controller responsible for the server
     * @param serverName the name of the server
     *
     * @return the status of the server following the start. Will not be <code>null</code>
     */
    ServerStatus startServer(String hostControllerName, String serverName);

    /**
     * Stops the given server.
     *
     * @param hostControllerName the name of the host controller responsible for the server
     * @param serverName the name of the server
     * @param gracefulShutdownTimeout maximum period to wait to allow the server
     *           to gracefully handle long running tasks before shutting down,
     *           or {@code -1} to shutdown immediately
     * @param timeUnit time unit in which {@code gracefulShutdownTimeout} is expressed
     *
     * @return the status of the server following the stop. Will not be <code>null</code>
     */
    ServerStatus stopServer(String hostControllerName, String serverName, long gracefulShutdownTimeout, TimeUnit timeUnit);

    /**
     * Restarts the given server.
     *
     * @param hostControllerName the name of the host controller responsible for the server
     * @param serverName the name of the server
     * @param gracefulShutdownTimeout maximum period to wait to allow the server
     *           to gracefully handle long running tasks before shutting down,
     *           or {@code -1} to shutdown immediately
     * @param timeUnit time unit in which {@code gracefulShutdownTimeout} is expressed
     *
     * @return the status of the server following the restart. Will not be <code>null</code>
     */
    ServerStatus restartServer(String hostControllerName, String serverName, long gracefulShutdownTimeout, TimeUnit timeUnit);

    /**
     * Factory used to create an {@link org.jboss.as.controller.client.helpers.domain.DomainClient} instance for a remote address
     * and port.
     */
    class Factory {
        /**
         * Create an {@link org.jboss.as.controller.client.helpers.domain.DomainClient} instance for a remote address and port.
         *
         * @param address The remote address to connect to
         * @param port The remote port
         * @return A domain client
         */
        public static DomainClient create(final InetAddress address, int port) {
            return new DomainClientImpl(address, port);
        }


        /**
         * Create an {@link org.jboss.as.controller.client.helpers.domain.DomainClient} instance for a remote address and port.
         *
         * @param address The remote address to connect to
         * @param port The remote port
         * @param handler CallbackHandler to prompt for authentication requirements.
         * @return A domain client
         */
        public static DomainClient create(final InetAddress address, int port, CallbackHandler handler) {
            return new DomainClientImpl(address, port, handler);
        }

    }
}
