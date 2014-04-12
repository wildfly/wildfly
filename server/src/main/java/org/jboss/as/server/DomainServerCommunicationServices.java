/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server;

import java.io.Serializable;
import java.net.InetSocketAddress;

import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.remoting.EndpointConfigFactory;
import org.jboss.as.remoting.EndpointService;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.mgmt.domain.HostControllerConnectionService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.RemotingOptions;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.xnio.OptionMap;

/**
 * Service activator for the communication services of a managed server in a domain.
 *
 * @author Emanuel Muckenhuber
 */
public class DomainServerCommunicationServices  implements ServiceActivator, Serializable {

    private static final OptionMap DEFAULTS = OptionMap.create(RemotingOptions.RECEIVE_WINDOW_SIZE, ProtocolChannelClient.Configuration.WINDOW_SIZE);

    private static final long serialVersionUID = 1593964083902839384L;

    // Shared operation ID for connection, this will get updated for start and reload
    private static volatile int initialOperationID;

    private final ModelNode endpointConfig;
    private final InetSocketAddress managementSocket;
    private final String serverName;
    private final String serverProcessName;
    private final byte[] authKey;

    private final boolean managementSubsystemEndpoint;

    DomainServerCommunicationServices(ModelNode endpointConfig, InetSocketAddress managementSocket, String serverName, String serverProcessName, byte[] authKey, boolean managementSubsystemEndpoint) {
        this.endpointConfig = endpointConfig;
        this.managementSocket = managementSocket;
        this.serverName = serverName;
        this.serverProcessName = serverProcessName;
        this.authKey = authKey;
        this.managementSubsystemEndpoint = managementSubsystemEndpoint;
    }

    static void updateOperationID(final int operationID) {
        initialOperationID = operationID;
    }

    @Override
    public void activate(final ServiceActivatorContext serviceActivatorContext) throws ServiceRegistryException {
        final ServiceTarget serviceTarget = serviceActivatorContext.getServiceTarget();
        final ServiceName endpointName = managementSubsystemEndpoint ? RemotingServices.SUBSYSTEM_ENDPOINT : ManagementRemotingServices.MANAGEMENT_ENDPOINT;
        final EndpointService.EndpointType endpointType = managementSubsystemEndpoint ? EndpointService.EndpointType.SUBSYSTEM : EndpointService.EndpointType.MANAGEMENT;
        try {
            // TODO see if we can figure out a way to work in the vault resolver instead of having to use ExpressionResolver.DEFAULT
            @SuppressWarnings("deprecation")
            final OptionMap options = EndpointConfigFactory.create(ExpressionResolver.DEFAULT, endpointConfig, DEFAULTS);
            ManagementRemotingServices.installRemotingManagementEndpoint(serviceTarget, endpointName, WildFlySecurityManager.getPropertyPrivileged(ServerEnvironment.NODE_NAME, null), endpointType, options, null, null);

            // Install the communication services
            final int port = managementSocket.getPort();
            final String host = NetworkUtils.canonize(managementSocket.getAddress().getHostAddress());
            HostControllerConnectionService service = new HostControllerConnectionService(host, port, serverName, serverProcessName, authKey, initialOperationID, managementSubsystemEndpoint);
            Services.addServerExecutorDependency(serviceTarget.addService(HostControllerConnectionService.SERVICE_NAME, service), service.getExecutorInjector(), false)
                    .addDependency(endpointName, Endpoint.class, service.getEndpointInjector())
                    .addDependency(ControlledProcessStateService.SERVICE_NAME, ControlledProcessStateService.class, service.getProcessStateServiceInjectedValue())
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();

        } catch (OperationFailedException e) {
            throw new ServiceRegistryException(e);
        }
    }

    /**
     * Create a new service activator for the domain server communication services.
     *
     * @param endpointConfig the endpoint configuration
     * @param managementSocket the management socket address
     * @param serverName the server name
     * @param serverProcessName the server process name
     * @param authKey the authentication key
     * @param managementSubsystemEndpoint whether to use the mgmt subsystem endpoint or not
     * @return the service activator
     */
    public static ServiceActivator create(final ModelNode endpointConfig, final InetSocketAddress managementSocket, final String serverName, final String serverProcessName,
                                          final byte[] authKey, final boolean managementSubsystemEndpoint) {

        return new DomainServerCommunicationServices(endpointConfig, managementSocket, serverName, serverProcessName, authKey, managementSubsystemEndpoint);
    }

    public interface OperationIDUpdater {

        /**
         * Update the operation ID when connecting to the HC.
         *
         * @param operationID the new operation ID
         */
        void updateOperationID(int operationID);

    }

}
