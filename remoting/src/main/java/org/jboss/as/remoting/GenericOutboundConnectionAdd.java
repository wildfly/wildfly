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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.remoting3.Endpoint;
import org.xnio.OptionMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.remoting.RemotingMessages.MESSAGES;

/**
 * @author Jaikiran Pai
 */
class GenericOutboundConnectionAdd extends AbstractOutboundConnectionAddHandler {

    static final GenericOutboundConnectionAdd INSTANCE = new GenericOutboundConnectionAdd();

    static ModelNode getAddOperation(final String connectionName, final String uri, PathAddress address) {
        if (connectionName == null || connectionName.trim().isEmpty()) {
            throw MESSAGES.connectionNameEmpty();
        }
        if (uri == null || uri.trim().isEmpty()) {
            throw MESSAGES.connectionUriEmpty(connectionName);
        }
        final ModelNode addOperation = new ModelNode();
        addOperation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        addOperation.get(ModelDescriptionConstants.OP_ADDR).set(address.toModelNode());

        // set the other params
        addOperation.get(CommonAttributes.URI).set(uri);

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
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                                  ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {
        final ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        final ServiceController serviceController = installRuntimeService(context, operation, fullModel, verificationHandler);
        newControllers.add(serviceController);
    }

    ServiceController installRuntimeService(final OperationContext context, final ModelNode operation, final ModelNode fullModel,
                                            ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        final PathAddress pathAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String connectionName = pathAddress.getLastElement().getValue();
        final OptionMap connectionCreationOptions = ConnectorResource.getOptions(fullModel.get(CommonAttributes.PROPERTY));


        //final OptionMap connectionCreationOptions = getConnectionCreationOptions(outboundConnection);
        // Get the destination URI
        final URI uri = getDestinationURI(context, operation);
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
            throw MESSAGES.couldNotCreateURI(new ModelNode().set("Cannot create a valid URI from " + uri + " -- " + e.toString()));
        }
    }
}
