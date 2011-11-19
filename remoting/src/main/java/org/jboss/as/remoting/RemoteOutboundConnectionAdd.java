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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FIXED_SOURCE_PORT;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

/**
 * @author Jaikiran Pai
 */
class RemoteOutboundConnectionAdd extends AbstractAddStepHandler {

    static final RemoteOutboundConnectionAdd INSTANCE = new RemoteOutboundConnectionAdd();

    static ModelNode getAddOperation(final String connectionName) {
        if (connectionName == null || connectionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Connection name cannot be null or empty");
        }
        final ModelNode addOperation = new ModelNode();
        addOperation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(CommonAttributes.REMOTE_OUTBOUND_CONNECTION, connectionName));
        addOperation.get(ModelDescriptionConstants.OP_ADDR).set(address.toModelNode());

        return addOperation;
    }

    private RemoteOutboundConnectionAdd() {

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        final String connectionName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
        model.get(CommonAttributes.NAME).set(connectionName);

        model.get(CommonAttributes.OUTBOUND_SOCKET_BINDING_REF).set(operation.get(CommonAttributes.OUTBOUND_SOCKET_BINDING_REF));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final ServiceController serviceController = installRuntimeService(context, model, verificationHandler);
        newControllers.add(serviceController);
    }

    ServiceController installRuntimeService(OperationContext context, ModelNode remoteOutboundConnection,
                                  ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        final String connectionName = remoteOutboundConnection.require(CommonAttributes.NAME).asString();
        final String outboundSocketBindingRef = remoteOutboundConnection.require(CommonAttributes.OUTBOUND_SOCKET_BINDING_REF).asString();

//        // create the pool config
//        final PoolConfig strictMaxPoolConfig = new StrictMaxPoolConfig(connectionName, maxPoolSize, timeout, TimeUnit.valueOf(unit));
//        // create and install the service
//        final PoolConfigService poolConfigService = new PoolConfigService(strictMaxPoolConfig);
//        final ServiceName serviceName = PoolConfigService.EJB_POOL_CONFIG_BASE_SERVICE_NAME.append(connectionName);
//        ServiceBuilder<PoolConfig> svcBuilder = context.getServiceTarget().addService(serviceName, poolConfigService);
//        if (verificationHandler != null) {
//            svcBuilder.addListener(verificationHandler);
//        }
//        return svcBuilder.install();
        return null;
    }
}
