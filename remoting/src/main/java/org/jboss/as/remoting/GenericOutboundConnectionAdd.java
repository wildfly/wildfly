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

import org.jboss.as.controller.AbstractAddStepHandler;
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

/**
 * @author Jaikiran Pai
 */
class GenericOutboundConnectionAdd extends AbstractAddStepHandler {

    static final GenericOutboundConnectionAdd INSTANCE = new GenericOutboundConnectionAdd();

    static ModelNode getAddOperation(final String connectionName) {
        if (connectionName == null || connectionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Connection name cannot be null or empty");
        }
        final ModelNode addOperation = new ModelNode();
        addOperation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        // /subsystem=remoting/outbound-connection=<connection-name>
        final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME),
                PathElement.pathElement(CommonAttributes.OUTBOUND_CONNECTION, connectionName));
        addOperation.get(ModelDescriptionConstants.OP_ADDR).set(address.toModelNode());

        return addOperation;
    }

    private GenericOutboundConnectionAdd() {

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        final String connectionName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
        model.get(CommonAttributes.NAME).set(connectionName);

        GenericOutboundConnectionResourceDefinition.URI.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final ServiceController serviceController = installRuntimeService(context, model, verificationHandler);
        newControllers.add(serviceController);
    }

    ServiceController installRuntimeService(OperationContext context, ModelNode outboundConnection,
                                            ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        final String connectionName = outboundConnection.require(CommonAttributes.NAME).asString();
        final String uri = outboundConnection.require(CommonAttributes.URI).asString();

        // create the service
        final GenericOutboundConnectionService outboundRemotingConnectionService;
        try {
            outboundRemotingConnectionService = new GenericOutboundConnectionService(new URI(uri));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Cannot create outbound connection service for connection named " + connectionName
                    + " with uri" + uri, e);
        }
        final ServiceName serviceName = AbstractOutboundConnectionService.OUTBOUND_CONNECTION_BASE_SERVICE_NAME.append(connectionName);
        // also add a alias service name to easily distinguish between a generic, remote and local type of connection services
        final ServiceName aliasServiceName = GenericOutboundConnectionService.OUTBOUND_CONNECTION_BASE_SERVICE_NAME.append(connectionName);
        final ServiceBuilder<GenericOutboundConnectionService> svcBuilder = context.getServiceTarget().addService(serviceName, outboundRemotingConnectionService)
                .addAliases(aliasServiceName)
                .addDependency(RemotingServices.SUBSYSTEM_ENDPOINT, Endpoint.class, outboundRemotingConnectionService.getEnpointInjector());

        if (verificationHandler != null) {
            svcBuilder.addListener(verificationHandler);
        }
        return svcBuilder.install();
    }
}
