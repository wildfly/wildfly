/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.remoting;

import static org.jboss.msc.service.ServiceController.Mode.ACTIVE;

import java.util.List;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.xnio.OptionMap;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RemotingServices {

    /** The name of the remoting service */
    public static final ServiceName REMOTING_BASE = ServiceName.JBOSS.append("remoting");

    /** The name of the endpoint service installed by the remoting subsystem */
    public static final ServiceName SUBSYSTEM_ENDPOINT = REMOTING_BASE.append("endpoint", "subsystem");

    /** The base name of the connector services */
    private static final ServiceName CONNECTOR_BASE = REMOTING_BASE.append("connector");

    /** The base name of the stream server services */
    private static final ServiceName SERVER_BASE = REMOTING_BASE.append("server");

    /**
     * Create the service name for a connector
     *
     * @param connectorName
     *            the connector name
     * @return the service name
     */
    public static ServiceName connectorServiceName(final String connectorName) {
        return CONNECTOR_BASE.append(connectorName);
    }

    /**
     * Create the service name for a stream server
     *
     * @param connectorName
     *            the connector name
     * @return the service name
     */
    public static ServiceName serverServiceName(final String connectorName) {
        return SERVER_BASE.append(connectorName);
    }

    /**
     * Create the service name for a channel
     *
     * @param channelName
     *            the channel name
     * @return the service name
     */
    public static ServiceName channelServiceName(final ServiceName endpointName, final String channelName) {
        return endpointName.append("channel").append(channelName);
    }

    /**
     * Installs the subystem remoting endpoint service. THis should only be called when installing a server's remoting subsystem
     *
     * @param serviceTarget the service target to install the services into
     * @param hostName the name of this host
     */
    public static void installSubsystemRemotingEndpoint(final ServiceTarget serviceTarget,
                                                        final String hostName,
                                                        final ServiceVerificationHandler verificationHandler,
                                                        final List<ServiceController<?>> newControllers) {
        installRemotingEndpoint(serviceTarget, SUBSYSTEM_ENDPOINT, hostName, EndpointService.EndpointType.SUBSYSTEM, verificationHandler, newControllers);
    }

    public static void addController(final List<ServiceController<?>> newControllers,
            final ServiceVerificationHandler verificationHandler, final ServiceBuilder<?> builder) {
        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        ServiceController<?> controller = builder.install();
        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    public static void installRemotingEndpoint(final ServiceTarget serviceTarget, final ServiceName endpointName,
            final String hostName, final EndpointService.EndpointType type, final ServiceVerificationHandler verificationHandler,
            final List<ServiceController<?>> newControllers) {
        EndpointService endpointService = new EndpointService(hostName, type);
        endpointService.setOptionMap(OptionMap.EMPTY);
        addController(newControllers, verificationHandler, serviceTarget.addService(endpointName, endpointService)
                .setInitialMode(ACTIVE));
    }

    public static void installConnectorServicesForNetworkInterfaceBinding(ServiceTarget serviceTarget,
            final ServiceName endpointName,
            final String connectorName,
            final ServiceName networkInterfaceBindingName,
            final int port,
            final OptionMap connectorPropertiesOptionMap,
            final ServiceVerificationHandler verificationHandler,
            final List<ServiceController<?>> newControllers) {
        installConnectorServices(serviceTarget, endpointName, connectorName, networkInterfaceBindingName, port, true, connectorPropertiesOptionMap, verificationHandler, newControllers);
    }

    public static void installConnectorServicesForSocketBinding(ServiceTarget serviceTarget,
            final ServiceName endpointName,
            final String connectorName,
            final ServiceName socketBindingName,
            final OptionMap connectorPropertiesOptionMap,
            final ServiceVerificationHandler verificationHandler,
            final List<ServiceController<?>> newControllers) {
        installConnectorServices(serviceTarget, endpointName, connectorName, socketBindingName, 0, false, connectorPropertiesOptionMap, verificationHandler, newControllers);
    }

    public static void installSecurityServices(ServiceTarget serviceTarget,
                                                 final String connectorName,
                                                 final ServiceName securityRealmName,
                                                 final ServiceName serverCallbackServiceName,
                                                 final ServiceName tmpDirService,
                                                 final ServiceVerificationHandler verificationHandler,
                                                 final List<ServiceController<?>> newControllers) {
        final ServiceName securityProviderName = RealmSecurityProviderService.createName(connectorName);

        final RealmSecurityProviderService rsps = new RealmSecurityProviderService();
        ServiceBuilder<?> builder = serviceTarget.addService(securityProviderName, rsps);
        if (securityRealmName != null) {
            builder.addDependency(securityRealmName, SecurityRealm.class, rsps.getSecurityRealmInjectedValue());
        }
        if (serverCallbackServiceName != null) {
            builder.addDependency(serverCallbackServiceName, CallbackHandler.class, rsps.getServerCallbackValue());
        }
        builder.addDependency(tmpDirService, String.class, rsps.getTmpDirValue());
        addController(newControllers, verificationHandler, builder);
    }

    private static void installConnectorServices(ServiceTarget serviceTarget,
                final ServiceName endpointName,
                final String connectorName,
                final ServiceName bindingName,
                final int port,
                final boolean isNetworkInterfaceBinding,
                final OptionMap connectorPropertiesOptionMap,
                final ServiceVerificationHandler verificationHandler,
                final List<ServiceController<?>> newControllers) {

        final ServiceName securityProviderName = RealmSecurityProviderService.createName(connectorName);
        if (isNetworkInterfaceBinding) {
            final InjectedNetworkBindingStreamServerService streamServerService = new InjectedNetworkBindingStreamServerService(connectorPropertiesOptionMap, port);
            addController(newControllers,
                    verificationHandler,
                    serviceTarget.addService(serverServiceName(connectorName), streamServerService)
                        .addDependency(securityProviderName, RemotingSecurityProvider.class, streamServerService.getSecurityProviderInjector())
                        .addDependency(endpointName, Endpoint.class, streamServerService.getEndpointInjector())
                        .addDependency(bindingName, NetworkInterfaceBinding.class, streamServerService.getInterfaceBindingInjector())
                        .addDependency(ServiceBuilder.DependencyType.OPTIONAL, SocketBindingManager.SOCKET_BINDING_MANAGER, SocketBindingManager.class, streamServerService.getSocketBindingManagerInjector()));
        } else {
            final InjectedSocketBindingStreamServerService streamServerService = new InjectedSocketBindingStreamServerService(connectorPropertiesOptionMap);
            addController(newControllers,
                    verificationHandler,
                    serviceTarget.addService(serverServiceName(connectorName), streamServerService)
                        .addDependency(securityProviderName, RemotingSecurityProvider.class, streamServerService.getSecurityProviderInjector())
                        .addDependency(endpointName, Endpoint.class, streamServerService.getEndpointInjector())
                        .addDependency(bindingName, SocketBinding.class, streamServerService.getSocketBindingInjector())
                        .addDependency(SocketBindingManager.SOCKET_BINDING_MANAGER, SocketBindingManager.class, streamServerService.getSocketBindingManagerInjector()));
        }
    }

    public static void removeConnectorServices(final OperationContext context, final String connectorName) {
        final ServiceName securityProviderName = RealmSecurityProviderService.createName(connectorName);
        context.removeService(serverServiceName(connectorName));
        context.removeService(securityProviderName);
    }
}
