/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.logging.CommonAttributes.HANDLER_TYPE;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.OVERFLOW_ACTION;
import static org.jboss.as.logging.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.logging.CommonAttributes.SUBHANDLERS;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Emanuel Muckenhuber
 */
class NewAsyncHandlerAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

    static final NewAsyncHandlerAdd INSTANCE = new NewAsyncHandlerAdd();

    /** {@inheritDoc} */
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode address = operation.get(OP_ADDR);
        final String name = address.get(address.asInt() - 1).asString();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
        compensatingOperation.get(OP).set(REMOVE);

        final String handlerType = operation.require(HANDLER_TYPE).asString();
        final LoggerHandlerType type = LoggerHandlerType.valueOf(handlerType);
        if(type != LoggerHandlerType.ASYNC_HANDLER) {
            resultHandler.handleFailed(new ModelNode().set("invalid operation for handler-type: " + type));
        }

        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
            final ServiceTarget serviceTarget = runtimeContext.getServiceTarget();
            try {
                final AsyncHandlerService service = new AsyncHandlerService();
                final ServiceBuilder<Handler> serviceBuilder = serviceTarget.addService(LogServices.handlerName(name), service);
                final List<InjectedValue<Handler>> list = new ArrayList<InjectedValue<Handler>>();
                for(final ModelNode handlerName : operation.get(SUBHANDLERS).asList()) {
                    final InjectedValue<Handler> injectedValue = new InjectedValue<Handler>();
                    serviceBuilder.addDependency(LogServices.handlerName(handlerName.asString()), Handler.class, injectedValue);
                    list.add(injectedValue);
                }
                service.addHandlers(list);
                service.setQueueLength(operation.get(QUEUE_LENGTH).asInt());
                service.setLevel(Level.parse(operation.get(LEVEL).asString()));
                service.setOverflowAction(OverflowAction.valueOf(operation.get(OVERFLOW_ACTION).asString()));
                serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
                serviceBuilder.install();
            } catch(Throwable t) {
                resultHandler.handleFailed(new ModelNode().set(t.getLocalizedMessage()));
                return Cancellable.NULL;
            }
        }

        final ModelNode subModel = context.getSubModel();
        subModel.get(QUEUE_LENGTH).set(operation.get(QUEUE_LENGTH));
        subModel.get(SUBHANDLERS).set(operation.get(SUBHANDLERS));
        subModel.get(LEVEL).set(operation.get(LEVEL));
        subModel.get(OVERFLOW_ACTION).set(operation.get(OVERFLOW_ACTION));

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
