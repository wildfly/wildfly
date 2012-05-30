/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller.operations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.host.controller.DomainModelControllerService;
import org.jboss.as.host.controller.jmx.RemotingConnectorService;
import org.jboss.as.host.controller.mgmt.ServerToHostOperationHandlerFactoryService;
import org.jboss.as.host.controller.resources.NativeManagementResourceDefinition;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.remoting.EndpointService;
import org.jboss.as.remoting.management.ManagementChannelRegistryService;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.RemotingOptions;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class NativeManagementAddHandler extends AbstractAddStepHandler {

    private static final int heartbeatInterval = 15000;
    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;
    private static final int WINDOW_SIZE = ProtocolChannelClient.Configuration.WINDOW_SIZE;

    private static final OptionMap SERVICE_OPTIONS = OptionMap.create(RemotingOptions.TRANSMIT_WINDOW_SIZE, WINDOW_SIZE,
                                                        RemotingOptions.RECEIVE_WINDOW_SIZE, WINDOW_SIZE);

    private static final OptionMap CONNECTION_OPTIONS = OptionMap.create(RemotingOptions.HEARTBEAT_INTERVAL, heartbeatInterval,
                                                        Options.READ_TIMEOUT, 45000);

    private final LocalHostControllerInfoImpl hostControllerInfo;


    public NativeManagementAddHandler(final LocalHostControllerInfoImpl hostControllerInfo) {
        this.hostControllerInfo = hostControllerInfo;
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        for (AttributeDefinition attr : NativeManagementResourceDefinition.ATTRIBUTE_DEFINITIONS) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler,
                                  final List<ServiceController<?>> newControllers) throws OperationFailedException {

        populateHostControllerInfo(hostControllerInfo, context, model);
        final ServiceTarget serviceTarget = context.getServiceTarget();

        ManagementChannelRegistryService.addService(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT);
        ManagementRemotingServices.installRemotingEndpoint(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                hostControllerInfo.getLocalHostName(), EndpointService.EndpointType.MANAGEMENT, CONNECTION_OPTIONS, null, null);

        final boolean onDemand = context.isBooting();
        installNativeManagementServices(serviceTarget, hostControllerInfo, verificationHandler, newControllers, onDemand);
    }

    static void populateHostControllerInfo(LocalHostControllerInfoImpl hostControllerInfo, OperationContext context, ModelNode model) throws OperationFailedException {
        hostControllerInfo.setNativeManagementInterface(NativeManagementResourceDefinition.INTERFACE.resolveModelAttribute(context, model).asString());
        final ModelNode portNode = NativeManagementResourceDefinition.NATIVE_PORT.resolveModelAttribute(context, model);
        hostControllerInfo.setNativeManagementPort(portNode.isDefined() ? portNode.asInt() : -1);
        final ModelNode realmNode = NativeManagementResourceDefinition.SECURITY_REALM.resolveModelAttribute(context, model);
        hostControllerInfo.setNativeManagementSecurityRealm(realmNode.isDefined() ? realmNode.asString() : null);
    }

    public static void installNativeManagementServices(final ServiceTarget serviceTarget, final LocalHostControllerInfo hostControllerInfo,
                                                       final ServiceVerificationHandler verificationHandler,
                                                       final List<ServiceController<?>> newControllers,
                                                       final boolean onDemand) {

        ServiceName realmSvcName = null;
        String nativeSecurityRealm = hostControllerInfo.getNativeManagementSecurityRealm();
        if (nativeSecurityRealm != null) {
            realmSvcName = SecurityRealmService.BASE_SERVICE_NAME.append(nativeSecurityRealm);
        }

        final ServiceName nativeManagementInterfaceBinding =
                NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(hostControllerInfo.getNativeManagementInterface());

        ManagementRemotingServices.installDomainConnectorServices(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                nativeManagementInterfaceBinding, hostControllerInfo.getNativeManagementPort(), realmSvcName, CONNECTION_OPTIONS, verificationHandler, newControllers);

        ManagementRemotingServices.installManagementChannelOpenListenerService(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                ManagementRemotingServices.SERVER_CHANNEL,
                ServerToHostOperationHandlerFactoryService.SERVICE_NAME, SERVICE_OPTIONS, verificationHandler, newControllers, onDemand);

        ManagementRemotingServices.installManagementChannelServices(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT,
                new ModelControllerClientOperationHandlerFactoryService(),
                DomainModelControllerService.SERVICE_NAME, ManagementRemotingServices.MANAGEMENT_CHANNEL, verificationHandler, newControllers);

        RemotingConnectorService.addService(serviceTarget, verificationHandler);
    }

}
