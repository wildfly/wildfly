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

package org.jboss.as.remoting;


import static org.jboss.msc.service.ServiceController.Mode.ACTIVE;
import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;

import javax.security.auth.callback.CallbackHandler;
import java.security.AccessController;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.remote.ManagementOperationHandlerFactory;
import org.jboss.as.controller.remote.AbstractModelControllerOperationHandlerFactoryService;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.network.NetworkInterfaceBinding;
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
 * Utility class to add remoting services
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:kkhan@redhat.com">Kabir Khan</a>
 */
public final class RemotingServices {
    private RemotingServices() {
    }

    /** The name of the remoting service */
    public static final ServiceName REMOTING = ServiceName.JBOSS.append("remoting");

    /** The name of the AuthenticationProvider service */
    public static final ServiceName AUTHENTICATION_PROVIDER = REMOTING.append("authentication_provider");

    /** The name of the service which provides the OptionMap */
    public static final ServiceName OPTION_MAP = AUTHENTICATION_PROVIDER.append("option_map");

    /** The name of the endpoint service */
    public static final ServiceName ENDPOINT = REMOTING.append("endpoint");

    /** The base name of the connector services */
    public static final ServiceName CONNECTOR = REMOTING.append("connector");

    /** The base name of the stream server services */
    public static final ServiceName SERVER = REMOTING.append("server");

    /** The base name of the channel open listener services */
    public static final ServiceName CHANNEL = REMOTING.append("channel");

    /** The name of the external management channel */
    public static final String MANAGEMENT_CHANNEL = "management";

    /** The name of the channel used between slave and master DCs */
    public static final String DOMAIN_CHANNEL = "domain";

    /** The name of the channel used for Server to HC comms */
    public static final String SERVER_CHANNEL = "server";

    private static final long EXECUTOR_KEEP_ALIVE_TIME = 60000;
    private static final int EXECUTOR_MAX_THREADS = 20;

    public static ExecutorService createExecutor() {
        ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("Remoting"), Boolean.FALSE, null, "Remoting %f thread %t", null, null, AccessController.getContext());

        QueueExecutor executor = new QueueExecutor(EXECUTOR_MAX_THREADS / 4 + 1, EXECUTOR_MAX_THREADS, EXECUTOR_KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, 500, threadFactory, true, null);

        return JBossExecutors.protectedExecutorService(executor);
    }

    /**
     * Create the service name for a connector
     *
     * @param connectorName the connector name
     * @return the service name
     */
    public static ServiceName connectorServiceName(final String connectorName) {
        return CONNECTOR.append(connectorName);
    }

    /**
     * Create the service name for a stream server
     *
     * @param address the host name
     * @param port the port
     * @return the service name
     */
    public static ServiceName serverServiceName(final String address, final int port) {
        return SERVER.append(address).append(String.valueOf(port));
    }

    /**
     * Create the service name for a channel
     *
     * @param channelName the channel name
     * @return the service name
     */
    public static ServiceName channelServiceName(final String channelName) {
        return CHANNEL.append(channelName);
    }

    /**
     * Create the service name for an operation handler
     *
     * @param controllerName the controller name name
     * @param channelName the name of the channel this operation handler should handle operations for
     * @return the service name
     */
    public static ServiceName operationHandlerName(ServiceName controllerName, String channelName) {
        return controllerName.append(channelName).append(ModelControllerClientOperationHandlerFactoryService.OPERATION_HANDLER_NAME_SUFFIX);
    }

    /**
     * Installs the remoting endpoint service. This is ONLY needed for domain mode,
     * standalone does it from its standalone.xml
     *
     * @param serviceTarget the service target to install the services into
     */
    public static void installRemotingEndpoint(ServiceTarget serviceTarget) {
        EndpointService endpointService = new EndpointService();
        endpointService.setOptionMap(OptionMap.EMPTY);
        final Injector<Executor> executorInjector = endpointService.getExecutorInjector();
        //TODO inject this from somewhere?
        executorInjector.inject(Executors.newCachedThreadPool());
        serviceTarget.addService(RemotingServices.ENDPOINT, endpointService)
                //.addDependency(ThreadsServices.executorName(threadPoolName), new CastingInjector<Executor>(executorInjector, Executor.class))
                //.addListener(verificationHandler)
                .setInitialMode(ACTIVE)
                .install();
    }

    /**
     * Installs the remoting stream server for a domain instance
     *
     * @param serviceTarget the service target to install the services into
     * @param networkInterfaceBinding the network interface binding
     * @param port the port
     * @param verificationHandler
     * @param newControllers
     */
    public static void installDomainConnectorServices(ServiceTarget serviceTarget,
                                                      final NetworkInterfaceBinding networkInterfaceBinding,
                                                      final int port,
                                                      final ServiceName securityRealmName,
                                                      final ServiceVerificationHandler verificationHandler,
                                                      final List<ServiceController<?>> newControllers) {
        ServiceName serverCallbackService = ServiceName.JBOSS.append("host", "controller", "server-inventory", "callback");
        installConnectorServices(serviceTarget, null, networkInterfaceBinding, port, securityRealmName, serverCallbackService, verificationHandler, newControllers);
    }

    /**
     * Removes the remoting stream server for a domain instance and then reinstalls it.
     *
     * @param operationContext context of the operation that is triggering the re-install
     * @param networkInterfaceBinding the network interface binding
     * @param port the port
     */
    public static void reinstallDomainConnectorServices(final OperationContext operationContext,
            final NetworkInterfaceBinding networkInterfaceBinding,
            final int port,
            final ServiceName securityRealmName) {
        remoteConnectorServices(operationContext, port);
        installDomainConnectorServices(operationContext.getServiceTarget(), networkInterfaceBinding, port, securityRealmName, null, null);
    }

    /**
     * Installs the remoting stream server for a standalone instance
     *
     * @param serviceTarget the service target to install the services into
     * @param networkInterfaceBindingName the name of the network interface binding
     * @param port the port
     * @param verificationHandler the verification handler
     * @param newControllers list to add the new services to
     */
    public static void installStandaloneConnectorServices(ServiceTarget serviceTarget,
            final ServiceName networkInterfaceBindingName,
            final int port,
            final ServiceName securityRealmName,
            final ServiceVerificationHandler verificationHandler,
            final List<ServiceController<?>> newControllers) {
        installConnectorServices(serviceTarget, networkInterfaceBindingName, null, port, securityRealmName, null, verificationHandler, newControllers);
    }

    /**
     * Set up the services to create a channel listener. This assumes that an endpoint called {@link #ENDPOINT} exists.
     *
     * @param serviceTarget the service target to install the services into
     * @param channelName the name of the channel
     * @param operationHandlerName the name of the operation handler to handle request for this channel
     * @param verificationHandler
     * @param newControllers list to add the new services to
     */
    public static void installChannelOpenListenerService(
            final ServiceTarget serviceTarget,
            final String channelName,
            final ServiceName operationHandlerName,
            final ServiceVerificationHandler verificationHandler,
            final List<ServiceController<?>> newControllers) {

        final ChannelOpenListenerService channelOpenListenerService = new ChannelOpenListenerService(channelName, OptionMap.EMPTY);
        ServiceBuilder<?> builder = serviceTarget.addService(channelOpenListenerService.getServiceName(), channelOpenListenerService)
            .addDependency(RemotingServices.ENDPOINT, Endpoint.class, channelOpenListenerService.getEndpointInjector())
            .addDependency(operationHandlerName, ManagementOperationHandlerFactory.class, channelOpenListenerService.getOperationHandlerInjector())
            .setInitialMode(ACTIVE);
         addController(newControllers, verificationHandler, builder);
    }

    /**
     * Set up the services to create a channel listener and operation handler service. This assumes that an endpoint called {@link #ENDPOINT} exists.
     *
     * @param serviceTarget the service target to install the services into
     * @param channelName the name of the channel
     * @param verificationHandler
     * @param newControllers list to add the new services to
     */
    public static void installChannelServices(
            final ServiceTarget serviceTarget,
            final AbstractModelControllerOperationHandlerFactoryService<?> operationHandlerService,
            final ServiceName modelControllerName,
            final String channelName,
            final ServiceVerificationHandler verificationHandler,
            final List<ServiceController<?>> newControllers) {

        final ServiceName operationHandlerName = operationHandlerName(modelControllerName, channelName);

        ServiceBuilder<?> builder = serviceTarget.addService(operationHandlerName, operationHandlerService)
            .addDependency(modelControllerName, ModelController.class, operationHandlerService.getModelControllerInjector())
            .setInitialMode(ACTIVE);
        addController(newControllers, verificationHandler, builder);

        installChannelOpenListenerService(serviceTarget, channelName, operationHandlerName, verificationHandler, newControllers);
    }

    private static void addController(List<ServiceController<?>> newControllers, ServiceVerificationHandler verificationHandler, ServiceBuilder<?> builder) {
        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        ServiceController<?> controller = builder.install();
        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    private static void installConnectorServices(ServiceTarget serviceTarget,
                                                 final ServiceName networkInterfaceBindingName,
                                                 final NetworkInterfaceBinding networkInterfaceBinding,
                                                 final int port,
                                                 final ServiceName securityRealmName,
                                                 final ServiceName serverCallbackService,
                                                 final ServiceVerificationHandler verificationHandler,
                                                 final List<ServiceController<?>> newControllers) {

        RealmAuthenticationProviderService raps = new RealmAuthenticationProviderService();
        ServiceBuilder<?> builder = serviceTarget.addService(AUTHENTICATION_PROVIDER, raps);
        if (securityRealmName != null) {
            builder.addDependency(securityRealmName, SecurityRealm.class, raps.getSecurityRealmInjectedValue());
        }
        if (serverCallbackService != null) {
            builder.addDependency(serverCallbackService, CallbackHandler.class, raps.getServerCallbackValue());
        }
        builder.setInitialMode(ON_DEMAND)
                .install();

        RealmOptionMapService roms = new RealmOptionMapService();
        serviceTarget.addService(OPTION_MAP, roms)
                .addDependency(AUTHENTICATION_PROVIDER, RealmAuthenticationProvider.class, roms.getRealmAuthenticationProviderInjectedValue())
                .setInitialMode(ON_DEMAND)
                .install();

        if (networkInterfaceBindingName != null) {
            final InjectedNetworkBindingStreamServerService streamServerService = new InjectedNetworkBindingStreamServerService(port);
            builder = serviceTarget.addService(RemotingServices.serverServiceName(MANAGEMENT_CHANNEL, port), streamServerService)
                    .addDependency(AUTHENTICATION_PROVIDER, ServerAuthenticationProvider.class, streamServerService.getAuthenticationProviderInjector())
                    .addDependency(OPTION_MAP, OptionMap.class, streamServerService.getOptionMapInjectedValue())
                    .addDependency(RemotingServices.ENDPOINT, Endpoint.class, streamServerService.getEndpointInjector())
                    .addDependency(networkInterfaceBindingName, NetworkInterfaceBinding.class, streamServerService.getInterfaceBindingInjector())
                    .setInitialMode(ACTIVE);
            addController(newControllers, verificationHandler, builder);
        } else {
            final NetworkBindingStreamServerService streamServerService = new NetworkBindingStreamServerService(networkInterfaceBinding, port);
            builder = serviceTarget.addService(RemotingServices.serverServiceName(MANAGEMENT_CHANNEL, port), streamServerService)
                    .addDependency(AUTHENTICATION_PROVIDER, ServerAuthenticationProvider.class, streamServerService.getAuthenticationProviderInjector())
                    .addDependency(OPTION_MAP, OptionMap.class, streamServerService.getOptionMapInjectedValue())
                    .addDependency(RemotingServices.ENDPOINT, Endpoint.class, streamServerService.getEndpointInjector())
                    .setInitialMode(ACTIVE);
            addController(newControllers, verificationHandler, builder);
        }
    }

    private static void remoteConnectorServices(final OperationContext context, final int port) {
        context.removeService(RemotingServices.serverServiceName(MANAGEMENT_CHANNEL, port));
        context.removeService(OPTION_MAP);
        context.removeService(AUTHENTICATION_PROVIDER);
    }
}
