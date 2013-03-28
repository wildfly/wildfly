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

package org.jboss.as.controller.client.helpers.standalone;

import javax.security.auth.callback.CallbackHandler;
import java.io.Closeable;
import java.net.InetAddress;
import java.util.concurrent.Future;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.impl.ModelControllerClientServerDeploymentManager;

/**
 * Primary deployment interface for a standalone JBoss AS instance.
 *
 * @author Brian Stansberry
 */
public interface ServerDeploymentManager extends Closeable {

    /**
     * Factory used to create an {@link ServerDeploymentManager} instance.
     */
    class Factory {

        /**
         * Create an {@link ServerDeploymentManager} instance for a remote address and port.
         *
         * This creates a {@code ModelControllerClient} which has to be closed using the
         * {@link org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager#close()} method.
         *
         * @param address The remote address to connect to
         * @param port The remote port
         * @return A domain client
         */
        public static ServerDeploymentManager create(final InetAddress address, int port) {
            return create(ModelControllerClient.Factory.create(address, port), true);
        }

        /**
         * Create an {@link ServerDeploymentManager} instance for a remote address and port.
         *
         * This creates a {@code ModelControllerClient} which has to be closed using the
         * {@link org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager#close()} method.
         *
         * @param protocol The protocol to use
         * @param address The remote address to connect to
         * @param port The remote port
         * @return A domain client
         */
        public static ServerDeploymentManager create(final String protocol, final InetAddress address, int port) {
            return create(ModelControllerClient.Factory.create(protocol,  address, port), true);
        }

        /**
         * Create an {@link ServerDeploymentManager} instance for a remote address and port.
         *
         * This creates a {@code ModelControllerClient} which has to be closed using the
         * {@link org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager#close()} method.
         *
         * @param address The remote address to connect to
         * @param port The remote port
         * @param handler The CallbackHandler for authentication
         * @return A domain client
         */
        public static ServerDeploymentManager create(final InetAddress address, int port, CallbackHandler handler) {
            return create(ModelControllerClient.Factory.create(address, port, handler), true);
        }
        /**
         * Create an {@link ServerDeploymentManager} instance for a remote address and port.
         *
         * This creates a {@code ModelControllerClient} which has to be closed using the
         * {@link org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager#close()} method.
         *
         * @param protocol The protocol to use
         * @param address The remote address to connect to
         * @param port The remote port
         * @param handler The CallbackHandler for authentication
         * @return A domain client
         */
        public static ServerDeploymentManager create(final String protocol, final InetAddress address, int port, CallbackHandler handler) {
            return create(ModelControllerClient.Factory.create(protocol, address, port, handler), true);
        }

        /**
         * Create an {@link ServerDeploymentManager} instance using the given {@link org.jboss.as.controller.client.ModelControllerClient}.
         *
         * @param client the client
         * @return A domain client
         */
        public static ServerDeploymentManager create(final ModelControllerClient client) {
            return create(client, false);
        }

        static ServerDeploymentManager create(final ModelControllerClient client, final boolean closeClient) {
            return new ModelControllerClientServerDeploymentManager(client, closeClient);
        }
    }

    /**
     * Initiates the creation of a new {@link DeploymentPlan}.
     *
     * @return builder object for the {@link DeploymentPlan}
     */
    InitialDeploymentPlanBuilder newDeploymentPlan();

    /**
     * Execute the deployment plan.
     *
     * @param plan the deployment plan
     *
     * @return {@link Future} from which the results of the deployment plan can
     *         be obtained
     */
    Future<ServerDeploymentPlanResult> execute(DeploymentPlan plan);
}
