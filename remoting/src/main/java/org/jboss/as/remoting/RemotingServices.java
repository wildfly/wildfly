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
import org.jboss.as.controller.remote.ModelControllerClientOperationHandlerService;
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
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class RemotingServices {
    private RemotingServices() {
    }

    public static final ServiceName REMOTING = ServiceName.JBOSS.append("remoting");
    public static final ServiceName ENDPOINT = REMOTING.append("endpoint");
    public static final ServiceName CONNECTOR = REMOTING.append("connector");
    public static final ServiceName SERVER = REMOTING.append("server");
    public static final ServiceName CHANNEL = REMOTING.append("channel");

    public static ServiceName connectorServiceName(final String connectorName) {
        return CONNECTOR.append(connectorName);
    }

    public static ServiceName serverServiceName(final String address, final int port) {
        return SERVER.append(address).append(String.valueOf(port));
    }

    public static ServiceName channelServiceName(final String channelName) {
        return CHANNEL.append(channelName);
    }

    public static ServiceName operationHandlerName(ServiceName controllerName, String channelName) {
        return controllerName.append(channelName).append(ModelControllerClientOperationHandlerService.OPERATION_HANDLER_NAME_SUFFIX);
    }

    public static void installStandaloneManagementChannelServices(
            final ServiceTarget serviceTarget,
            final ModelControllerClientOperationHandlerService operationHandlerService,
            final ServiceName modelControllerName,
            final ServiceName networkInterfaceBindingName,
            final int port,
            List<ServiceController<?>> newControllers) {
        installServices(serviceTarget, operationHandlerService, modelControllerName, networkInterfaceBindingName, port, newControllers);
    }

    public static void installDomainControllerManagementChannelServices(
            final ServiceTarget serviceTarget,
            final ModelControllerClientOperationHandlerService operationHandlerService,
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

    private static void installServices(
            final ServiceTarget serviceTarget,
            final ModelControllerClientOperationHandlerService operationHandlerService,
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

    public static void installChannelServices(
            final ServiceTarget serviceTarget,
            final ModelControllerClientOperationHandlerService operationHandlerService,
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
}
