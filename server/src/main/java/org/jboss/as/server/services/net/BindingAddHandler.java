/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.server.services.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FIXED_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import org.jboss.as.controller.operations.common.SocketBindingAddHandler;
import org.jboss.as.controller.operations.validation.InetAddressValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.resource.AbstractSocketBindingResourceDefinition;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handler for the server socket-binding resource's add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BindingAddHandler extends SocketBindingAddHandler {

    public static final BindingAddHandler INSTANCE = new BindingAddHandler();

    private BindingAddHandler() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        String name = address.getLastElement().getValue();

        try {
            newControllers.add(installBindingService(context, model, name));
        } catch (UnknownHostException e) {
            throw new OperationFailedException(new ModelNode().set(e.getLocalizedMessage()));
        }

    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

    public static ServiceController<SocketBinding> installBindingService(OperationContext context, ModelNode config, String name)
            throws UnknownHostException, OperationFailedException {
        final ServiceTarget serviceTarget = context.getServiceTarget();

        final ModelNode intfNode = AbstractSocketBindingResourceDefinition.INTERFACE.resolveModelAttribute(context, config);
        final String intf = intfNode.isDefined() ? intfNode.asString() : null;
        final int port = AbstractSocketBindingResourceDefinition.PORT.resolveModelAttribute(context, config).asInt();
        final boolean fixedPort = AbstractSocketBindingResourceDefinition.FIXED_PORT.resolveModelAttribute(context, config).asBoolean();
        final ModelNode mcastNode = AbstractSocketBindingResourceDefinition.MULTICAST_ADDRESS.resolveModelAttribute(context, config);
        final String mcastAddr = mcastNode.isDefined() ? mcastNode.asString() : null;
        final int mcastPort = AbstractSocketBindingResourceDefinition.MULTICAST_PORT.resolveModelAttribute(context, config).asInt(0);
        final InetAddress mcastInet = mcastAddr == null ? null : InetAddress.getByName(mcastAddr);

        final SocketBindingService service = new SocketBindingService(name, port, fixedPort, mcastInet, mcastPort);
        final ServiceBuilder<SocketBinding> builder = serviceTarget.addService(SocketBinding.JBOSS_BINDING_NAME.append(name), service);
        if (intf != null) {
            builder.addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(intf), NetworkInterfaceBinding.class, service.getInterfaceBinding());
        }
        return builder.addDependency(SocketBindingManager.SOCKET_BINDING_MANAGER, SocketBindingManager.class, service.getSocketBindings())
                .setInitialMode(Mode.ON_DEMAND)
                .install();
    }
}
