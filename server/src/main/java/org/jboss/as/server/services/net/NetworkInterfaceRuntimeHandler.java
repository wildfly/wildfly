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

import java.net.InetAddress;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

/**
 * {@code OperationStepHandler} for the runtime attributes of a network interface.
 *
 * @author Emanuel Muckenhuber
 */
public class NetworkInterfaceRuntimeHandler implements OperationStepHandler {

    public static final OperationStepHandler INSTANCE = new NetworkInterfaceRuntimeHandler();

    public static final SimpleAttributeDefinition RESOLVED_ADDRESS = new SimpleAttributeDefinitionBuilder("resolved-address", ModelType.STRING)
            .setStorageRuntime()
            .build();

    protected NetworkInterfaceRuntimeHandler() {
        //
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        final String interfaceName = address.getLastElement().getValue();
        final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                final ServiceController<?> controller = context.getServiceRegistry(false).getService(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName));
                if(controller != null && controller.getState() == ServiceController.State.UP) {
                    final NetworkInterfaceBinding binding = NetworkInterfaceBinding.class.cast(controller.getValue());
                    final InetAddress address = binding.getAddress();
                    final ModelNode result = new ModelNode();
                    if(RESOLVED_ADDRESS.getName().equals(attributeName)) {
                        result.set(address.getHostAddress());
                    }
                    context.getResult().set(result);
                }
                context.stepCompleted();
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
