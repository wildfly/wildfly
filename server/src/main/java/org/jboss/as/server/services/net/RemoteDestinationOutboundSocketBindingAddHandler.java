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

package org.jboss.as.server.services.net;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FIXED_SOURCE_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOURCE_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOURCE_PORT;

import java.net.UnknownHostException;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handles "add" operation for remote-destination outbound-socket-binding
 *
 * @author Jaikiran Pai
 */
public class RemoteDestinationOutboundSocketBindingAddHandler extends AbstractAddStepHandler {

    static final RemoteDestinationOutboundSocketBindingAddHandler INSTANCE = new RemoteDestinationOutboundSocketBindingAddHandler();

    public static ModelNode getOperation(final ModelNode address, final ModelNode remoteDestinationOutboundSocketBinding) {
        final ModelNode addOperation = new ModelNode();
        addOperation.get(OP).set(ADD);
        addOperation.get(OP_ADDR).set(address);
        // destination host
        addOperation.get(HOST).set(remoteDestinationOutboundSocketBinding.get(HOST));

        // destination port
        addOperation.get(PORT).set(remoteDestinationOutboundSocketBinding.get(PORT));

        // (optional) source interface
        if (remoteDestinationOutboundSocketBinding.get(SOURCE_INTERFACE).isDefined()) {
            addOperation.get(SOURCE_INTERFACE).set(remoteDestinationOutboundSocketBinding.get(SOURCE_INTERFACE));
        }
        // (optional) source port
        if (remoteDestinationOutboundSocketBinding.get(SOURCE_PORT).isDefined()) {
            addOperation.get(SOURCE_PORT).set(remoteDestinationOutboundSocketBinding.get(SOURCE_PORT));
        }
        if (remoteDestinationOutboundSocketBinding.get(FIXED_SOURCE_PORT).isDefined()) {
            addOperation.get(FIXED_SOURCE_PORT).set(remoteDestinationOutboundSocketBinding.get(FIXED_SOURCE_PORT));
        }
        return addOperation;
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {

        model.get(ModelDescriptionConstants.HOST).set(operation.get(ModelDescriptionConstants.HOST));
        model.get(ModelDescriptionConstants.PORT).set(operation.get(ModelDescriptionConstants.PORT));
        if (operation.hasDefined(ModelDescriptionConstants.SOURCE_INTERFACE)) {
            model.get(ModelDescriptionConstants.SOURCE_INTERFACE).set(operation.get(ModelDescriptionConstants.SOURCE_INTERFACE));
        }
        if (operation.hasDefined(ModelDescriptionConstants.SOURCE_PORT)) {
            model.get(ModelDescriptionConstants.SOURCE_PORT).set(operation.get(ModelDescriptionConstants.SOURCE_PORT));
        }
        if (operation.hasDefined(FIXED_SOURCE_PORT)) {
            model.get(ModelDescriptionConstants.FIXED_SOURCE_PORT).set(operation.get(FIXED_SOURCE_PORT));
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> serviceControllers) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String outboundSocketName = address.getLastElement().getValue();
        final ServiceController<OutboundSocketBinding> outboundSocketBindingServiceController;
        try {
            outboundSocketBindingServiceController = this.installOutboundSocketBindingService(context, model, outboundSocketName);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        serviceControllers.add(outboundSocketBindingServiceController);
    }

    public static ServiceController<OutboundSocketBinding> installOutboundSocketBindingService(final OperationContext context, final ModelNode model,
                                                                                               final String outboundSocketName) throws OperationFailedException, UnknownHostException {
        final ServiceTarget serviceTarget = context.getServiceTarget();

        // destination host
        final String destinationHost = RemoteDestinationOutboundSocketBindingResourceDefinition.HOST.resolveModelAttribute(context, model).asString();
        // port
        final int destinationPort = RemoteDestinationOutboundSocketBindingResourceDefinition.PORT.resolveModelAttribute(context, model).asInt();

        // (optional) source interface
        final ModelNode sourceInterfaceModelNode = OutboundSocketBindingResourceDefinition.SOURCE_INTERFACE.resolveModelAttribute(context, model);
        final String sourceInterfaceName = sourceInterfaceModelNode.isDefined() ? sourceInterfaceModelNode.asString() : null;
        // (optional) source port
        final ModelNode sourcePortModelNode = OutboundSocketBindingResourceDefinition.SOURCE_PORT.resolveModelAttribute(context, model);
        final Integer sourcePort = sourcePortModelNode.isDefined() ? sourcePortModelNode.asInt() : null;
        // (optional) fixedSourcePort
        final ModelNode fixedSourcePortModelNode = OutboundSocketBindingResourceDefinition.FIXED_SOURCE_PORT.resolveModelAttribute(context, model);
        final boolean fixedSourcePort = fixedSourcePortModelNode.isDefined() ? fixedSourcePortModelNode.asBoolean() : false;
        // create the service
        final OutboundSocketBindingService outboundSocketBindingService = new RemoteDestinationOutboundSocketBindingService(outboundSocketName, destinationHost, destinationPort, sourcePort, fixedSourcePort);
        final ServiceBuilder<OutboundSocketBinding> serviceBuilder = serviceTarget.addService(OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(outboundSocketName), outboundSocketBindingService);
        // if a source interface has been specified then add a dependency on it
        if (sourceInterfaceName != null) {
            serviceBuilder.addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(sourceInterfaceName), NetworkInterfaceBinding.class, outboundSocketBindingService.getSourceNetworkInterfaceBindingInjector());
        }
        // add a dependency on the socket binding manager
        serviceBuilder.addDependency(SocketBindingManager.SOCKET_BINDING_MANAGER, SocketBindingManager.class, outboundSocketBindingService.getSocketBindingManagerInjector());
        // install the service
        return serviceBuilder.setInitialMode(ServiceController.Mode.ON_DEMAND).install();
    }
}
