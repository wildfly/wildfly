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
import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.HANDLER_TYPE;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.PATH;
import static org.jboss.as.logging.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.logging.CommonAttributes.RELATIVE_TO;

import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Emanuel Muckenhuber
 */
class NewFileHandlerAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

    static final NewFileHandlerAdd INSTANCE = new NewFileHandlerAdd();

    /** {@inheritDoc} */
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode address = operation.get(OP_ADDR);
        final String name = address.get(address.asInt() - 1).asString();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
        compensatingOperation.get(OP).set(REMOVE);

        final String handlerType = operation.require(HANDLER_TYPE).asString();
        final LoggerHandlerType type = LoggerHandlerType.valueOf(handlerType);
        if(type != LoggerHandlerType.FILE_HANDLER) {
            resultHandler.handleFailed(new ModelNode().set("invalid operation for handler-type: " + type));
        }
        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
            final ServiceTarget serviceTarget = runtimeContext.getServiceTarget();
            try {
                final FileHandlerService service = new FileHandlerService();
                final ServiceBuilder<Handler> serviceBuilder = serviceTarget.addService(LogServices.handlerName(name), service);
                if (operation.has(FILE)) {
                    if(operation.get(FILE).has(RELATIVE_TO)) {
                        serviceBuilder.addDependency(AbstractPathService.pathNameOf(operation.get(FILE, RELATIVE_TO).asString()), String.class, service.getRelativeToInjector());
                    }
                    service.setPath(operation.get(FILE, PATH).asString());
                }
                service.setLevel(Level.parse(operation.get(LEVEL).asString()));
                final Boolean autoFlush = operation.get(AUTOFLUSH).asBoolean();
                if (autoFlush != null) service.setAutoflush(autoFlush.booleanValue());
                service.setEncoding(operation.get(ENCODING).asString());
                service.setFormatterSpec(createFormatterSpec(operation));
                serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
                serviceBuilder.install();
            } catch(Throwable t) {
                resultHandler.handleFailed(new ModelNode().set(t.getLocalizedMessage()));
                return Cancellable.NULL;
            }
        }

        final ModelNode subModel = context.getSubModel();
        subModel.get(AUTOFLUSH).set(operation.get(AUTOFLUSH));
        subModel.get(ENCODING).set(operation.get(ENCODING));
        subModel.get(FORMATTER).set(operation.get(FORMATTER));
        subModel.get(LEVEL).set(operation.get(LEVEL));
        subModel.get(FILE).set(operation.get(FILE));
        subModel.get(QUEUE_LENGTH).set(operation.get(QUEUE_LENGTH));

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

    static AbstractFormatterSpec createFormatterSpec(final ModelNode operation) {
        return new PatternFormatterSpec(operation.get(FORMATTER).asString());
    }
}
