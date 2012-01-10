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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.remoting3.Endpoint;
import org.xnio.OptionMap;

/**
 * @author Jaikiran Pai
 */
class GenericOutboundConnectionAdd extends AbstractOutboundConnectionAddHandler {

    static final GenericOutboundConnectionAdd INSTANCE = new GenericOutboundConnectionAdd();

    static ModelNode getAddOperation(final String connectionName, final String uri, final Map<String, String> connectionCreationOptions) {
        if (connectionName == null || connectionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Connection name cannot be null or empty");
        }
        if (uri == null || uri.trim().isEmpty()) {
            throw new IllegalArgumentException("Connection URI cannot be null for connection named " + connectionName);
        }
        final ModelNode addOperation = new ModelNode();
        addOperation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        // /subsystem=remoting/outbound-connection=<connection-name>
        final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME),
                PathElement.pathElement(CommonAttributes.OUTBOUND_CONNECTION, connectionName));
        addOperation.get(ModelDescriptionConstants.OP_ADDR).set(address.toModelNode());

        // set the other params
        addOperation.get(CommonAttributes.URI).set(uri);
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

    private GenericOutboundConnectionAdd() {

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        super.populateModel(operation, model);
        GenericOutboundConnectionResourceDefinition.URI.validateAndSet(operation, model);

    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ServiceController serviceController = installRuntimeService(context, name, model, verificationHandler);
        newControllers.add(serviceController);
    }

    ServiceController installRuntimeService(OperationContext context, final String connectionName, ModelNode outboundConnection,
                                            ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        // fetch the connection creation options from the model
        final OptionMap connectionCreationOptions = getConnectionCreationOptions(outboundConnection);
        // Get the destination URI
        final URI uri = getDestinationURI(context, outboundConnection);
        // create the service
        final GenericOutboundConnectionService outboundRemotingConnectionService = new GenericOutboundConnectionService(connectionName, uri, connectionCreationOptions);
        final ServiceName serviceName = AbstractOutboundConnectionService.OUTBOUND_CONNECTION_BASE_SERVICE_NAME.append(connectionName);
        // also add a alias service name to easily distinguish between a generic, remote and local type of connection services
        final ServiceName aliasServiceName = GenericOutboundConnectionService.GENERIC_OUTBOUND_CONNECTION_BASE_SERVICE_NAME.append(connectionName);
        final ServiceBuilder<GenericOutboundConnectionService> svcBuilder = context.getServiceTarget().addService(serviceName, outboundRemotingConnectionService)
                .addAliases(aliasServiceName)
                .addDependency(RemotingServices.SUBSYSTEM_ENDPOINT, Endpoint.class, outboundRemotingConnectionService.getEnpointInjector());

        if (verificationHandler != null) {
            svcBuilder.addListener(verificationHandler);
        }
        return svcBuilder.install();
    }

    URI getDestinationURI(final OperationContext context, final ModelNode outboundConnection) throws OperationFailedException {
        final String uri = GenericOutboundConnectionResourceDefinition.URI.resolveModelAttribute(context, outboundConnection).asString();
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new OperationFailedException(new ModelNode().set("Cannot create a valid URI from " + uri + " -- " + e.toString()));
        }
    }
}
