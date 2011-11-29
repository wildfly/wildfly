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

package org.jboss.as.server.operations;

import static org.jboss.as.server.mgmt.NativeManagementResourceDefinition.ATTRIBUTE_DEFINITIONS;
import static org.jboss.as.server.mgmt.NativeManagementResourceDefinition.INTERFACE;
import static org.jboss.as.server.mgmt.NativeManagementResourceDefinition.NATIVE_PORT;
import static org.jboss.as.server.mgmt.NativeManagementResourceDefinition.SECURITY_REALM;
import static org.jboss.as.server.mgmt.NativeManagementResourceDefinition.SOCKET_BINDING;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.remoting.EndpointService;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.Services;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.xnio.OptionMap;


/**
 * The Add handler for the Native Interface when running a standalone server.
 *
 * @author Kabir Khan
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class NativeManagementAddHandler extends AbstractAddStepHandler {

    public static final NativeManagementAddHandler INSTANCE = new NativeManagementAddHandler();
    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition definition : ATTRIBUTE_DEFINITIONS) {
            validateAndSet(definition, operation, model);
        }
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                  ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {


        final ServiceTarget serviceTarget = context.getServiceTarget();

        final ServiceName endpointName = ManagementRemotingServices.MANAGEMENT_ENDPOINT;
        final String hostName = SecurityActions.getSystemProperty(ServerEnvironment.NODE_NAME);
        ManagementRemotingServices.installRemotingEndpoint(serviceTarget, ManagementRemotingServices.MANAGEMENT_ENDPOINT, hostName, EndpointService.EndpointType.MANAGEMENT, verificationHandler, newControllers);
        installNativeManagementConnector(context, model, endpointName, serviceTarget, verificationHandler, newControllers);

        ManagementRemotingServices.installManagementChannelServices(serviceTarget,
                endpointName,
                new ModelControllerClientOperationHandlerFactoryService(),
                Services.JBOSS_SERVER_CONTROLLER,
                ManagementRemotingServices.MANAGEMENT_CHANNEL,
                verificationHandler,
                newControllers);
    }

    // TODO move this kind of logic into AttributeDefinition itself
    private static void validateAndSet(final AttributeDefinition definition, final ModelNode operation, final ModelNode subModel) throws OperationFailedException {
        final String attributeName = definition.getName();
        final boolean has = operation.has(attributeName);
        if(! has && definition.isRequired(operation)) {
            throw new OperationFailedException(new ModelNode().set(String.format("%s is required", attributeName)));
        }
        if(has) {
            if(! definition.isAllowed(operation)) {
                throw new OperationFailedException(new ModelNode().set(String.format("%s is not allowed when [%s] are present", attributeName, definition.getAlternatives())));
            }
            definition.validateAndSet(operation, subModel);
        } else {
            // create the undefined node
            subModel.get(definition.getName());
        }
    }

    // TODO move this kind of logic into AttributeDefinition itself
    private static ModelNode validateResolvedModel(final AttributeDefinition definition, final OperationContext context,
                                                   final ModelNode subModel) throws OperationFailedException {
        final String attributeName = definition.getName();
        final boolean has = subModel.has(attributeName);
        if(! has && definition.isRequired(subModel)) {
            throw new OperationFailedException(new ModelNode().set(String.format("%s is required", attributeName)));
        }
        ModelNode result;
        if(has) {
            if(! definition.isAllowed(subModel)) {
                if (subModel.hasDefined(attributeName)) {
                    throw new OperationFailedException(new ModelNode().set(String.format("%s is not allowed when [%s] are present", attributeName, definition.getAlternatives())));
                } else {
                    // create the undefined node
                    result = new ModelNode();
                }
            } else {
                result = definition.resolveModelAttribute(context, subModel);
            }
        } else {
            // create the undefined node
            result = new ModelNode();
        }

        return result;
    }

    static void installNativeManagementConnector(final OperationContext context, final ModelNode model, final ServiceName endpointName, final ServiceTarget serviceTarget,
                                                 final ServiceVerificationHandler verificationHandler,
                                                 final List<ServiceController<?>> newControllers) throws OperationFailedException {

        ServiceName socketBindingServiceName = null;
        ServiceName interfaceSvcName = null;
        int port = 0;
        final ModelNode socketBindingNode = validateResolvedModel(SOCKET_BINDING, context, model);
        if (socketBindingNode.isDefined()) {
            final String bindingName = SOCKET_BINDING.resolveModelAttribute(context, model).asString();
            socketBindingServiceName = SocketBinding.JBOSS_BINDING_NAME.append(bindingName);
        } else {
            String interfaceName = INTERFACE.resolveModelAttribute(context, model).asString();
            interfaceSvcName = NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName);
            port = NATIVE_PORT.resolveModelAttribute(context, model).asInt();
        }

        ServiceName realmSvcName = null;
        final ModelNode realmNode = SECURITY_REALM.resolveModelAttribute(context, model);
        if (realmNode.isDefined()) {
            realmSvcName = SecurityRealmService.BASE_SERVICE_NAME.append(realmNode.asString());
        } else {
            Logger.getLogger("org.jboss.as").warn("No security realm defined for native management service, all access will be unrestricted.");
        }

        ServiceName tmpDirPath = ServiceName.JBOSS.append("server", "path", "jboss.server.temp.dir");
        RemotingServices.installSecurityServices(serviceTarget, ManagementRemotingServices.MANAGEMENT_CONNECTOR, realmSvcName, null, tmpDirPath, verificationHandler, newControllers);
        if (socketBindingServiceName == null) {
            ManagementRemotingServices.installConnectorServicesForNetworkInterfaceBinding(serviceTarget, endpointName,
                    ManagementRemotingServices.MANAGEMENT_CONNECTOR, interfaceSvcName, port, OptionMap.EMPTY, verificationHandler, newControllers);
        } else {
            ManagementRemotingServices.installConnectorServicesForSocketBinding(serviceTarget, endpointName,
                    ManagementRemotingServices.MANAGEMENT_CONNECTOR,
                    socketBindingServiceName, OptionMap.EMPTY, verificationHandler, newControllers);
        }
    }

}
