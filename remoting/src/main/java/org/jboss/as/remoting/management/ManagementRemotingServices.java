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

package org.jboss.as.remoting.management;


import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.mgmt.support.ManagementChannelInitialization;
import static org.jboss.msc.service.ServiceController.Mode.ACTIVE;
import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;

import java.util.List;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.remote.AbstractModelControllerOperationHandlerFactoryService;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.RemotingOptions;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * Utility class to add remoting services
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:kkhan@redhat.com">Kabir Khan</a>
 */
public final class ManagementRemotingServices extends RemotingServices {
    private ManagementRemotingServices() {
    }

    /** The name of the endpoint service used for management */
    public static final ServiceName MANAGEMENT_ENDPOINT = RemotingServices.REMOTING_BASE.append("endpoint", "management");

    /** The name of the external management channel */
    public static final String MANAGEMENT_CHANNEL = "management";

    /** The name of the channel used between slave and master DCs */
    public static final String DOMAIN_CHANNEL = "domain";

    /** The name of the channel used for Server to HC comms */
    public static final String SERVER_CHANNEL = "server";

    public static final String MANAGEMENT_CONNECTOR = "management";

    /**
     * Installs a remoting stream server for a domain instance
     *
     * @param serviceTarget the service target to install the services into
     * @param endpointName the name of the endpoint to install the stream server into
     * @param networkInterfaceBinding the network interface binding
     * @param port the port
     * @param securityRealmName the security real name
     * @param options the remoting options
     * @param verificationHandler
     * @param newControllers
     */
    public static void installDomainConnectorServices(final ServiceTarget serviceTarget,
                                                      final ServiceName endpointName,
                                                      final ServiceName networkInterfaceBinding,
                                                      final int port,
                                                      final ServiceName securityRealmName,
                                                      final OptionMap options,
                                                      final ServiceVerificationHandler verificationHandler,
                                                      final List<ServiceController<?>> newControllers) {
        ServiceName serverCallbackService = ServiceName.JBOSS.append("host", "controller", "server-inventory", "callback");
        ServiceName tmpDirPath = ServiceName.JBOSS.append("server", "path", "jboss.domain.temp.dir");
        installSecurityServices(serviceTarget, MANAGEMENT_CONNECTOR, securityRealmName, serverCallbackService, tmpDirPath, verificationHandler, newControllers);
        installConnectorServicesForNetworkInterfaceBinding(serviceTarget, endpointName, MANAGEMENT_CONNECTOR, networkInterfaceBinding, port, options, verificationHandler, newControllers);
    }

    /**
     * Set up the services to create a channel listener. This assumes that an endpoint service called {@code endpointName} exists.
     *
     * @param serviceTarget the service target to install the services into
     * @param endpointName the name of the endpoint to install a channel listener into
     * @param channelName the name of the channel
     * @param operationHandlerName the name of the operation handler to handle request for this channel
     * @param options the remoting options
     * @param verificationHandler
     * @param newControllers list to add the new services to
     * @param onDemand whether to install the services on demand
     */
    public static void installManagementChannelOpenListenerService(
            final ServiceTarget serviceTarget,
            final ServiceName endpointName,
            final String channelName,
            final ServiceName operationHandlerName,
            final OptionMap options,
            final ServiceVerificationHandler verificationHandler,
            final List<ServiceController<?>> newControllers,
            final boolean onDemand) {

        final ManagementChannelOpenListenerService channelOpenListenerService = new ManagementChannelOpenListenerService(channelName, options);
        final ServiceBuilder<?> builder = serviceTarget.addService(channelOpenListenerService.getServiceName(endpointName), channelOpenListenerService)
                .addDependency(endpointName, Endpoint.class, channelOpenListenerService.getEndpointInjector())
                .addDependency(operationHandlerName, ManagementChannelInitialization.class, channelOpenListenerService.getOperationHandlerInjector())
                .addDependency(ManagementChannelRegistryService.SERVICE_NAME, ManagementChannelRegistryService.class, channelOpenListenerService.getRegistry())
                .setInitialMode(onDemand ? ON_DEMAND : ACTIVE);

        addController(newControllers, verificationHandler, builder);
    }

    public static void removeManagementChannelOpenListenerService(final OperationContext context, final ServiceName endpointName, final String channelName) {
        context.removeService(RemotingServices.channelServiceName(endpointName, channelName));
    }

    /**
     * Set up the services to create a channel listener and operation handler service.
     *
     * @param serviceTarget the service target to install the services into
     * @param endpointName the endpoint name to install the services into
     * @param channelName the name of the channel
     * @param verificationHandler
     * @param newControllers list to add the new services to
     */
    public static void installManagementChannelServices(
            final ServiceTarget serviceTarget,
            final ServiceName endpointName,
            final AbstractModelControllerOperationHandlerFactoryService operationHandlerService,
            final ServiceName modelControllerName,
            final String channelName,
            final ServiceVerificationHandler verificationHandler,
            final List<ServiceController<?>> newControllers) {

        final OptionMap options = OptionMap.create(RemotingOptions.RECEIVE_WINDOW_SIZE, ProtocolChannelClient.Configuration.WINDOW_SIZE, RemotingOptions.TRANSMIT_WINDOW_SIZE, ProtocolChannelClient.Configuration.WINDOW_SIZE);
        final ServiceName operationHandlerName = endpointName.append(channelName).append(ModelControllerClientOperationHandlerFactoryService.OPERATION_HANDLER_NAME_SUFFIX);

        final ServiceBuilder<?> builder = serviceTarget.addService(operationHandlerName, operationHandlerService)
            .addDependency(modelControllerName, ModelController.class, operationHandlerService.getModelControllerInjector())
            .setInitialMode(ACTIVE);

        addController(newControllers, verificationHandler, builder);

        installManagementChannelOpenListenerService(serviceTarget, endpointName, channelName, operationHandlerName, options, verificationHandler, newControllers, false);
    }

    public static void removeManagementChannelServices(final OperationContext context, final ServiceName endpointName,
                                                       final String channelName) {
        removeManagementChannelOpenListenerService(context, endpointName, channelName);
        final ServiceName operationHandlerName = endpointName.append(channelName).append(ModelControllerClientOperationHandlerFactoryService.OPERATION_HANDLER_NAME_SUFFIX);
        context.removeService(operationHandlerName);
    }
}
