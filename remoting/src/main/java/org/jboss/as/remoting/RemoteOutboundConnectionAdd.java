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

package org.jboss.as.remoting;

import java.util.List;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.remoting3.Endpoint;
import org.xnio.OptionMap;

/**
 * @author Jaikiran Pai
 */
class RemoteOutboundConnectionAdd extends AbstractOutboundConnectionAddHandler {

    static final RemoteOutboundConnectionAdd INSTANCE = new RemoteOutboundConnectionAdd();

    static ModelNode getAddOperation(final String connectionName, final String outboundSocketBindingRef, final Map<String, String> connectionCreationOptions) {
        if (connectionName == null || connectionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Connection name cannot be null or empty");
        }
        if (outboundSocketBindingRef == null || outboundSocketBindingRef.trim().isEmpty()) {
            throw new IllegalArgumentException("Outbound socket binding reference cannot be null or empty for connection named " + connectionName);
        }
        final ModelNode addOperation = new ModelNode();
        addOperation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        // /subsystem=remoting/remote-outbound-connection=<connection-name>
        final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME),
                PathElement.pathElement(CommonAttributes.REMOTE_OUTBOUND_CONNECTION, connectionName));
        addOperation.get(ModelDescriptionConstants.OP_ADDR).set(address.toModelNode());

        // set the other params
        addOperation.get(CommonAttributes.OUTBOUND_SOCKET_BINDING_REF).set(outboundSocketBindingRef);
        // optional connection creation options
        if (connectionCreationOptions != null) {
            for (final Map.Entry<String, String> entry : connectionCreationOptions.entrySet()) {
                if (entry.getKey() == null) {
                    // skip
                    continue;
                }
                addOperation.get(CommonAttributes.CONNECTION_CREATION_OPTIONS).add(entry.getKey(), entry.getValue());
            }
        }

        return addOperation;
    }

    private RemoteOutboundConnectionAdd() {

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        super.populateModel(operation, model);

        RemoteOutboundConnnectionResourceDefinition.OUTBOUND_SOCKET_BINDING_REF.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ServiceController serviceController = installRuntimeService(context, name, model, verificationHandler);
        newControllers.add(serviceController);
    }

    ServiceController installRuntimeService(OperationContext context, String connectionName, ModelNode remoteOutboundConnection,
                                            ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        final String outboundSocketBindingRef = RemoteOutboundConnnectionResourceDefinition.OUTBOUND_SOCKET_BINDING_REF.resolveModelAttribute(context, remoteOutboundConnection).asString();
        final ServiceName outboundSocketBindingDependency = OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(outboundSocketBindingRef);
        // fetch the connection creation options from the model
        final OptionMap connectionCreationOptions = getConnectionCreationOptions(remoteOutboundConnection);
        // create the service
        final RemoteOutboundConnectionService outboundConnectionService = new RemoteOutboundConnectionService(connectionName, connectionCreationOptions);
        final ServiceName serviceName = AbstractOutboundConnectionService.OUTBOUND_CONNECTION_BASE_SERVICE_NAME.append(connectionName);
        // also add a alias service name to easily distinguish between a generic, remote and local type of connection services
        final ServiceName aliasServiceName = RemoteOutboundConnectionService.REMOTE_OUTBOUND_CONNECTION_BASE_SERVICE_NAME.append(connectionName);
        final ServiceBuilder<RemoteOutboundConnectionService> svcBuilder = context.getServiceTarget().addService(serviceName, outboundConnectionService)
                .addAliases(aliasServiceName)
                .addDependency(RemotingServices.SUBSYSTEM_ENDPOINT, Endpoint.class, outboundConnectionService.getEnpointInjector())
                .addDependency(outboundSocketBindingDependency, OutboundSocketBinding.class, outboundConnectionService.getDestinationOutboundSocketBindingInjector());

        if (verificationHandler != null) {
            svcBuilder.addListener(verificationHandler);
        }
        return svcBuilder.install();

    }
}
