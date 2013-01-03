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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.net.InetAddress;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * {@code SocketBinding} metric handlers.
 *
 * @author Emanuel Muckenhuber
 */
public final class BindingMetricHandlers {

    private static final ServiceName SOCKET_BINDING = SocketBinding.JBOSS_BINDING_NAME;

    abstract static class AbstractBindingMetricsHandler implements OperationStepHandler {

        /** {@inheritDoc} */
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final PathElement element = address.getLastElement();

            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final ModelNode result = context.getResult();
                    final ServiceController<?> controller = context.getServiceRegistry(false).getRequiredService(SOCKET_BINDING.append(element.getValue()));
                    if(controller != null && controller.getState() == ServiceController.State.UP) {
                        final SocketBinding binding = SocketBinding.class.cast(controller.getValue());
                        AbstractBindingMetricsHandler.this.execute(operation, binding, result);
                    } else {
                        result.set(getNoMetrics());
                    }

                    context.stepCompleted();
                }
            }, OperationContext.Stage.RUNTIME);

            context.stepCompleted();
        }

        abstract void execute(ModelNode operation, SocketBinding binding, ModelNode result);

        abstract ModelNode getNoMetrics();
    }

    public static class BoundHandler extends AbstractBindingMetricsHandler {

        public static final String ATTRIBUTE_NAME = "bound";
        public static final AttributeDefinition ATTRIBUTE_DEFINITION = new SimpleAttributeDefinition(ATTRIBUTE_NAME, ModelType.BOOLEAN, true);
        public static final OperationStepHandler INSTANCE = new BoundHandler();

        private BoundHandler() {
            //
        }

        @Override
        void execute(final ModelNode operation, final SocketBinding binding, final ModelNode result) {
            // The socket should be bound when it's registered at the SocketBindingManager
            result.set(binding.isBound());
        }

        ModelNode getNoMetrics() {
            return new ModelNode().set(false);
        }

    }

    public static class BoundAddressHandler extends AbstractBindingMetricsHandler {

        public static final String ATTRIBUTE_NAME = "bound-address";
        public static final AttributeDefinition ATTRIBUTE_DEFINITION = new SimpleAttributeDefinition(ATTRIBUTE_NAME, ModelType.STRING, true);
        public static final OperationStepHandler INSTANCE = new BoundAddressHandler();

        private BoundAddressHandler() {
            //
        }

        @Override
        void execute(final ModelNode operation, final SocketBinding binding, final ModelNode result) {
            ManagedBinding managedBinding = binding.getManagedBinding();
            if (managedBinding != null) {
                InetAddress addr = managedBinding.getBindAddress().getAddress();
                result.set(addr.getHostAddress());
            }
        }

        ModelNode getNoMetrics() {
            return new ModelNode();
        }
    }

    public static class BoundPortHandler extends AbstractBindingMetricsHandler {

        public static final String ATTRIBUTE_NAME = "bound-port";
        public static final AttributeDefinition ATTRIBUTE_DEFINITION =
                new SimpleAttributeDefinitionBuilder(ATTRIBUTE_NAME, ModelType.INT, true)
                        .setValidator(new IntRangeValidator(1, Integer.MAX_VALUE, true, false)).build();

        public static final OperationStepHandler INSTANCE = new BoundPortHandler();

        private BoundPortHandler() {
            //
        }

        @Override
        void execute(final ModelNode operation, final SocketBinding binding, final ModelNode result) {
            ManagedBinding managedBinding = binding.getManagedBinding();
            if (managedBinding != null) {
                int port = managedBinding.getBindAddress().getPort();
                result.set(port);
            }
        }

        ModelNode getNoMetrics() {
            return new ModelNode();
        }
    }

    private BindingMetricHandlers() {
        //
    }

}
