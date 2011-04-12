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

package org.jboss.as.server.operations.sockets;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.services.net.ManagedBinding;
import org.jboss.as.server.services.net.SocketBinding;
import org.jboss.as.server.services.net.SocketBindingManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * {@code SocketBinding} metric handlers.
 *
 * @author Emanuel Muckenhuber
 */
public final class BindingMetricHandlers {

    private static final ServiceName SOCKET_BINDING = SocketBinding.JBOSS_BINDING_NAME;
    private static final ModelNode NO_METRICS = new ModelNode().set("no metrics available");

    abstract static class AbstractBindingMetricsHandler implements ModelQueryOperationHandler {

        /** {@inheritDoc} */
        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final PathElement element = address.getLastElement();
            if(context.getRuntimeContext() != null) {
                context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                    @Override
                    public void execute(final RuntimeTaskContext context) throws OperationFailedException {
                        final ServiceController<?> controller = context.getServiceRegistry().getRequiredService(SOCKET_BINDING.append(element.getValue()));
                        if(controller != null) {
                            final SocketBinding binding = SocketBinding.class.cast(controller.getValue());
                            AbstractBindingMetricsHandler.this.execute(operation, binding, resultHandler);
                            resultHandler.handleResultComplete();
                        } else {
                            resultHandler.handleResultFragment(Util.NO_LOCATION, NO_METRICS);
                            resultHandler.handleResultComplete();
                        }
                    }
                });
            } else {
                resultHandler.handleResultFragment(Util.NO_LOCATION, NO_METRICS);
                resultHandler.handleResultComplete();
            }
            return new BasicOperationResult();
        }

        abstract void execute(ModelNode operation, SocketBinding binding, ResultHandler handler);
    }

    public static class BoundHandler extends AbstractBindingMetricsHandler {

        public static final String ATTRIBUTE_NAME = "bound";
        public static final OperationHandler INSTANCE = new BoundHandler();

        private BoundHandler() {
            //
        }

        @Override
        void execute(final ModelNode operation, final SocketBinding binding, final ResultHandler handler) {
            // The socket should be bound when it's registered at the SocketBindingManager
            handler.handleResultFragment(Util.NO_LOCATION, new ModelNode().set(binding.isBound()));
        }
    }

    private BindingMetricHandlers() {
        //
    }

}
