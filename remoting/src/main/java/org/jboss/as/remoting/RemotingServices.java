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
import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;

import java.security.AccessController;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.jboss.threads.JBossExecutors;
import org.jboss.threads.JBossThreadFactory;
import org.jboss.threads.QueueExecutor;
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

    private static final long EXECUTOR_KEEP_ALIVE_TIME = 60000;
    private static final int EXECUTOR_MAX_THREADS = 20;

    public static ExecutorService createExecutor() {
        ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("Remoting"), Boolean.FALSE, null,
                "Remoting %f thread %t", null, null, AccessController.getContext());

        QueueExecutor executor = new QueueExecutor(EXECUTOR_MAX_THREADS / 4 + 1, EXECUTOR_MAX_THREADS, EXECUTOR_KEEP_ALIVE_TIME,
                TimeUnit.MILLISECONDS, 500, threadFactory, true, null);

        return JBossExecutors.protectedExecutorService(executor);
    }

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
     * @param address
     *            the host name
     * @param port
     *            the port
     * @return the service name
     */
    public static ServiceName serverServiceName(final String address, final int port) {
        return SERVER_BASE.append(address).append(String.valueOf(port));
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

    protected static void installRemotingEndpoint(final ServiceTarget serviceTarget, final ServiceName endpointName,
            final String hostName, final EndpointService.EndpointType type, final ServiceVerificationHandler verificationHandler,
            final List<ServiceController<?>> newControllers) {
        EndpointService endpointService = new EndpointService(hostName, type);
        endpointService.setOptionMap(OptionMap.EMPTY);
        final Injector<Executor> executorInjector = endpointService.getExecutorInjector();
        // TODO inject this from somewhere?
        executorInjector.inject(Executors.newCachedThreadPool());
        addController(newControllers, verificationHandler, serviceTarget.addService(endpointName, endpointService)
                .setInitialMode(ACTIVE));
    }

    public static void installConnectorServicesForNetworkInterfaceBinding(ServiceTarget serviceTarget,
            final ServiceName endpointName,
            final String connectorName,
            final ServiceName networkInterfaceBindingName,
            final int port,
            final ServiceName securityRealmName,
            final ServiceName serverCallbackServiceName,
            final ServiceVerificationHandler verificationHandler,
            final List<ServiceController<?>> newControllers) {
        installConnectorServices(serviceTarget, endpointName, connectorName, networkInterfaceBindingName, null, port, null, securityRealmName, serverCallbackServiceName, verificationHandler, newControllers);
    }

    public static void installConnectorServicesForNetworkInterfaceBinding(ServiceTarget serviceTarget,
            final ServiceName endpointName,
            final String connectorName,
            final NetworkInterfaceBinding networkInterfaceBinding,
            final int port,
            final ServiceName securityRealmName,
            final ServiceName serverCallbackServiceName,
            final ServiceVerificationHandler verificationHandler,
            final List<ServiceController<?>> newControllers) {
        installConnectorServices(serviceTarget, endpointName, connectorName, null, networkInterfaceBinding, port, null, securityRealmName, serverCallbackServiceName, verificationHandler, newControllers);
    }

    public static void installConnectorServicesForSocketBinding(ServiceTarget serviceTarget,
            final ServiceName endpointName,
            final String connectorName,
            final ServiceName socketBindingName,
            final ServiceName securityRealmName,
            final ServiceName serverCallbackServiceName,
            final ServiceVerificationHandler verificationHandler,
            final List<ServiceController<?>> newControllers) {
        installConnectorServices(serviceTarget, endpointName, connectorName, null, null, 0, socketBindingName, securityRealmName, serverCallbackServiceName, verificationHandler, newControllers);
    }

    private static void installConnectorServices(ServiceTarget serviceTarget,
                                                 final ServiceName endpointName,
                                                 final String connectorName,
                                                 final ServiceName networkInterfaceBindingName,
                                                 final NetworkInterfaceBinding networkInterfaceBinding,
                                                 final int port,
                                                 final ServiceName socketBindingName,
                                                 final ServiceName securityRealmName,
                                                 final ServiceName serverCallbackServiceName,
                                                 final ServiceVerificationHandler verificationHandler,
                                                 final List<ServiceController<?>> newControllers) {

        final ServiceName authProviderName = RealmAuthenticationProviderService.createName(connectorName);
        final ServiceName optionMapName = RealmOptionMapService.createName(connectorName);

        final RealmAuthenticationProviderService raps = new RealmAuthenticationProviderService();
        ServiceBuilder<?> builder = serviceTarget.addService(authProviderName, raps);
        if (securityRealmName != null) {
            builder.addDependency(securityRealmName, SecurityRealm.class, raps.getSecurityRealmInjectedValue());
        } else {
            Logger.getLogger("org.jboss.as").warn("No security realm defined for native management service, all access will be unrestricted.");
        }
        if (serverCallbackServiceName != null) {
            builder.addDependency(serverCallbackServiceName, CallbackHandler.class, raps.getServerCallbackValue());
        }
        builder.setInitialMode(ON_DEMAND)
                .install();

        RealmOptionMapService roms = new RealmOptionMapService();
        serviceTarget.addService(optionMapName, roms)
                .addDependency(authProviderName, RealmAuthenticationProvider.class, roms.getRealmAuthenticationProviderInjectedValue())
                .setInitialMode(ON_DEMAND)
                .install();

        if (networkInterfaceBindingName != null) {
            final InjectedNetworkBindingStreamServerService streamServerService = new InjectedNetworkBindingStreamServerService(port);
            builder = serviceTarget.addService(serverServiceName(connectorName, port), streamServerService)
                    .addDependency(authProviderName, ServerAuthenticationProvider.class, streamServerService.getAuthenticationProviderInjector())
                    .addDependency(optionMapName, OptionMap.class, streamServerService.getOptionMapInjectedValue())
                    .addDependency(endpointName, Endpoint.class, streamServerService.getEndpointInjector())
                    .addDependency(networkInterfaceBindingName, NetworkInterfaceBinding.class, streamServerService.getInterfaceBindingInjector())
                    .setInitialMode(ACTIVE);
            addController(newControllers, verificationHandler, builder);
        } else if (socketBindingName != null) {
            final InjectedSocketBindingStreamServerService streamServerService = new InjectedSocketBindingStreamServerService();
            builder = serviceTarget.addService(serverServiceName(connectorName, port), streamServerService)
                    .addDependency(authProviderName, ServerAuthenticationProvider.class, streamServerService.getAuthenticationProviderInjector())
                    .addDependency(optionMapName, OptionMap.class, streamServerService.getOptionMapInjectedValue())
                    .addDependency(endpointName, Endpoint.class, streamServerService.getEndpointInjector())
                    .addDependency(socketBindingName, SocketBinding.class, streamServerService.getSocketBindingInjector())
                    .setInitialMode(ACTIVE);
            addController(newControllers, verificationHandler, builder);

        } else {
            final NetworkBindingStreamServerService streamServerService = new NetworkBindingStreamServerService(networkInterfaceBinding, port);
            builder = serviceTarget.addService(serverServiceName(connectorName, port), streamServerService)
                    .addDependency(authProviderName, ServerAuthenticationProvider.class, streamServerService.getAuthenticationProviderInjector())
                    .addDependency(optionMapName, OptionMap.class, streamServerService.getOptionMapInjectedValue())
                    .addDependency(endpointName, Endpoint.class, streamServerService.getEndpointInjector())
                    .setInitialMode(ACTIVE);
            addController(newControllers, verificationHandler, builder);
        }
    }

    protected static void removeConnectorServices(final OperationContext context, final String connectorName, final int port) {
        final ServiceName authProviderName = RealmAuthenticationProviderService.createName(connectorName);
        final ServiceName optionMapName = RealmOptionMapService.createName(connectorName);
        context.removeService(serverServiceName(connectorName, port));
        context.removeService(optionMapName);
        context.removeService(authProviderName);
    }
}
