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

import java.util.logging.Level;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Emanuel Muckenhuber
 */
class NewLoggerAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

    static final NewLoggerAdd INSTANCE = new NewLoggerAdd();

    /** {@inheritDoc} */
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
        compensatingOperation.get(OP).set(REMOVE);

        final String level = operation.require(CommonAttributes.LEVEL).asString();
        final ModelNode handlers = operation.get(CommonAttributes.HANDLERS);

        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
            final ServiceTarget target = runtimeContext.getServiceTarget();
            final String loggerName = name;
            try {
                // Install logger service
                final LoggerService service = new LoggerService(loggerName);
                service.setLevel(Level.parse(level));
                target.addService(LogServices.loggerName(loggerName), service)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            } catch (Throwable t) {
                resultHandler.handleFailed(new ModelNode().set(t.getLocalizedMessage()));
                return Cancellable.NULL;
            }
            try {
                // install logger handler services
                if(handlers.getType() != ModelType.UNDEFINED) {
                    LogServices.installLoggerHandlers(target, loggerName, handlers);
                }
            } catch (Throwable t) {
                resultHandler.handleFailed(new ModelNode().set(t.getLocalizedMessage()));
                return Cancellable.NULL;
            }
        }

        final ModelNode subModel = context.getSubModel();
        subModel.get(CommonAttributes.LEVEL).set(level);
        subModel.get(CommonAttributes.HANDLERS).set(handlers);

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }
}
