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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.security.SecurityRealmService;
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

    private RemoteOutboundConnectionAdd() {

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        super.populateModel(operation, model);

        RemoteOutboundConnectionResourceDefinition.OUTBOUND_SOCKET_BINDING_REF.validateAndSet(operation, model);
        RemoteOutboundConnectionResourceDefinition.USERNAME.validateAndSet(operation, model);
        RemoteOutboundConnectionResourceDefinition.SECURITY_REALM.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        final ServiceController serviceController = installRuntimeService(context, operation, fullModel, verificationHandler);
        newControllers.add(serviceController);
    }

    ServiceController installRuntimeService(final OperationContext context, final ModelNode operation, final ModelNode fullModel,
                                            ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        final String connectionName = address.getLastElement().getValue();

        final String outboundSocketBindingRef = RemoteOutboundConnectionResourceDefinition.OUTBOUND_SOCKET_BINDING_REF.resolveModelAttribute(context, operation).asString();
        final ServiceName outboundSocketBindingDependency = OutboundSocketBinding.OUTBOUND_SOCKET_BINDING_BASE_SERVICE_NAME.append(outboundSocketBindingRef);
        // fetch the connection creation options from the model
        final OptionMap connectionCreationOptions = ConnectorResource.getOptions(fullModel.get(CommonAttributes.PROPERTY));
        final String username = fullModel.hasDefined(CommonAttributes.USERNAME) ? fullModel.require(CommonAttributes.USERNAME).asString() : null;
        final String securityRealm = fullModel.hasDefined(CommonAttributes.SECURITY_REALM) ? fullModel.require(CommonAttributes.SECURITY_REALM).asString() : null;

        // create the service
        final RemoteOutboundConnectionService outboundConnectionService = new RemoteOutboundConnectionService(connectionName, connectionCreationOptions, username);
        final ServiceName serviceName = AbstractOutboundConnectionService.OUTBOUND_CONNECTION_BASE_SERVICE_NAME.append(connectionName);
        // also add a alias service name to easily distinguish between a generic, remote and local type of connection services
        final ServiceName aliasServiceName = RemoteOutboundConnectionService.REMOTE_OUTBOUND_CONNECTION_BASE_SERVICE_NAME.append(connectionName);
        final ServiceBuilder<RemoteOutboundConnectionService> svcBuilder = context.getServiceTarget().addService(serviceName, outboundConnectionService)
                .addAliases(aliasServiceName)
                .addDependency(RemotingServices.SUBSYSTEM_ENDPOINT, Endpoint.class, outboundConnectionService.getEndpointInjector())
                .addDependency(outboundSocketBindingDependency, OutboundSocketBinding.class, outboundConnectionService.getDestinationOutboundSocketBindingInjector());

        if (securityRealm != null) {
            final ServiceName secuirtyRealmName = SecurityRealmService.BASE_SERVICE_NAME.append(securityRealm);
            svcBuilder.addDependency(secuirtyRealmName, SecurityRealm.class, outboundConnectionService.getSecurityRealmInjector());
        }

        if (verificationHandler != null) {
            svcBuilder.addListener(verificationHandler);
        }
        return svcBuilder.install();

    }
}
