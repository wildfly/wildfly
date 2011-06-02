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

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.remote.NewAbstractModelControllerOperationHandlerService;
import org.jboss.as.controller.remote.NewModelControllerClientOperationHandlerService;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.security.SimpleServerAuthenticationProvider;
import org.xnio.ChannelListener;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Sequence;

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

    /** The name of the endpoint service */
    public static final ServiceName ENDPOINT = REMOTING.append("endpoint");

    /** The base name of the connector services */
    public static final ServiceName CONNECTOR = REMOTING.append("connector");

    /** The base name of the stream server services */
    public static final ServiceName SERVER = REMOTING.append("server");

    /** The base name of the channel open listener services */
    public static final ServiceName CHANNEL = REMOTING.append("channel");

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
        return controllerName.append(channelName).append(NewModelControllerClientOperationHandlerService.OPERATION_HANDLER_NAME_SUFFIX);
    }

    /**
     * Set up the remoting services for a standalone instanc needed for management.
     * This includes setting up the stream server listening on the management socket, and the main
     * managemenent channel and associated operation handler.
     *
     * @param serviceTarget the service target to install the services into
     * @param operationHandlerService the operation handler service
     * @param modelControllerName the name of the server controller
     * @param networkInterfaceBindingName the name of the network interface binding
     * @param port the port
     * @param newControllers list to add the new services to
     */
    public static void installStandaloneManagementChannelServices(
            final ServiceTarget serviceTarget,
            final NewModelControllerClientOperationHandlerService operationHandlerService,
            final ServiceName modelControllerName,
            final ServiceName networkInterfaceBindingName,
            final int port,
            List<ServiceController<?>> newControllers) {
        installServices(serviceTarget, operationHandlerService, modelControllerName, networkInterfaceBindingName, port, newControllers);
    }

    /**
     * Set up the remoting services for a domain controller instance needed for management.
     * This includes setting up the main endpoint, the stream server listening on the management socket, and the main
     * managemenent channel and associated operation handler.
     *
     * @param serviceTarget the service target to install the services into
     * @param operationHandlerService the operation handler service
     * @param modelControllerName the name of the domain controller
     * @param networkInterfaceBindingName the name of the network interface binding
     * @param port the port
     * @param newControllers list to add the new services to
     */
    public static void installDomainControllerManagementChannelServices(
            final ServiceTarget serviceTarget,
            final NewModelControllerClientOperationHandlerService operationHandlerService,
            final ServiceName modelControllerName,
            final ServiceName networkInterfaceBindingName,
            final int port,
            List<ServiceController<?>> newControllers) {

        EndpointService endpointService = new EndpointService();
        endpointService.setOptionMap(OptionMap.EMPTY);
        final Injector<Executor> executorInjector = endpointService.getExecutorInjector();
        //TODO inject this from somewhere?
        executorInjector.inject(Executors.newCachedThreadPool());
        addController(newControllers, serviceTarget.addService(RemotingServices.ENDPOINT, endpointService)
                //.addDependency(ThreadsServices.executorName(threadPoolName), new CastingInjector<Executor>(executorInjector, Executor.class))
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install());

        installServices(serviceTarget, operationHandlerService, modelControllerName, networkInterfaceBindingName, port, newControllers);
    }

    /**
     * Set up the services to create a channel listener. This assumes that an endpoint called {@link #ENDPOINT} exists.
     *
     * @param serviceTarget the service target to install the services into
     * @param channelName the name of the channel
     * @param operationHandlerName the name of the operation handler to handle request for this channel
     * @param newControllers list to add the new services to
     */
    public static void installChannelOpenListenerService(
            final ServiceTarget serviceTarget,
            final String channelName,
            final ServiceName operationHandlerName,
            List<ServiceController<?>> newControllers) {

        final ChannelOpenListenerService channelOpenListenerService = new ChannelOpenListenerService(channelName, OptionMap.EMPTY);
        addController(newControllers, serviceTarget.addService(channelOpenListenerService.getServiceName(), channelOpenListenerService)
            .addDependency(RemotingServices.ENDPOINT, Endpoint.class, channelOpenListenerService.getEndpointInjector())
            .addDependency(operationHandlerName, ManagementOperationHandler.class, channelOpenListenerService.getOperationHandlerInjector())
            .setInitialMode(Mode.ACTIVE)
            .install());
    }

    /**
     * Set up the services to create a channel listener and operation handler service. This assumes that an endpoint called {@link #ENDPOINT} exists.
     *
     * @param serviceTarget the service target to install the services into
     * @param channelName the name of the channel
     * @param operationHandlerName the name of the operation handler to handle request for this channel
     * @param newControllers list to add the new services to
     */
    public static void installChannelServices(
            final ServiceTarget serviceTarget,
            final NewAbstractModelControllerOperationHandlerService<?> operationHandlerService,
            final ServiceName modelControllerName,
            final String channelName,
            List<ServiceController<?>> newControllers) {

        final ServiceName operationHandlerName = operationHandlerName(modelControllerName, channelName);
        addController(newControllers, serviceTarget.addService(operationHandlerName, operationHandlerService)
            .addDependency(modelControllerName, NewModelController.class, operationHandlerService.getModelControllerInjector())
            .setInitialMode(Mode.ACTIVE)
            .install());

        installChannelOpenListenerService(serviceTarget, channelName, operationHandlerName, newControllers);
    }

    private static void addController(List<ServiceController<?>> newControllers, ServiceController<?> controller) {
        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    private static void installServices(
            final ServiceTarget serviceTarget,
            final NewAbstractModelControllerOperationHandlerService<?> operationHandlerService,
            final ServiceName modelControllerName,
            final ServiceName networkInterfaceBindingName,
            final int port,
            List<ServiceController<?>> newControllers) {

        //FIXME get this provider from somewhere
        //There is currently a probable bug in jboss remoting, so the user realm name MUST be the same as
        //the endpoint name.
        final SimpleServerAuthenticationProvider provider = new SimpleServerAuthenticationProvider();
        provider.addUser("bob", RemotingServices.ENDPOINT.getSimpleName(), "pass".toCharArray());

        final ConnectorService connectorService = new ConnectorService();
        //TODO replace these options with something better
        connectorService.setOptionMap(OptionMap.create(Options.SASL_MECHANISMS, Sequence.of("DIGEST-MD5")));
        addController(newControllers, serviceTarget.addService(RemotingServices.connectorServiceName("management"), connectorService)
            .addDependency(RemotingServices.ENDPOINT, Endpoint.class, connectorService.getEndpointInjector())
            .addInjection(connectorService.getAuthenticationProviderInjector(), provider)
            .setInitialMode(Mode.ACTIVE)
            .install());

        final NetworkBindingStreamServerService streamServerService = new NetworkBindingStreamServerService(port);
        addController(newControllers, serviceTarget.addService(RemotingServices.serverServiceName("management", port), streamServerService)
            .addDependency(RemotingServices.connectorServiceName("management"), ChannelListener.class, streamServerService.getConnectorInjector())
            .addDependency(networkInterfaceBindingName, NetworkInterfaceBinding.class, streamServerService.getInterfaceBindingInjector())
                    .setInitialMode(Mode.ACTIVE)
                    .install());

        installChannelServices(serviceTarget, operationHandlerService, modelControllerName, "management", newControllers);
    }

}
