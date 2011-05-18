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

package org.jboss.as.logging;

import java.io.UnsupportedEncodingException;
import java.util.logging.Handler;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.Level;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Parent operation responsible for updating the common attributes of logging handlers.
 *
 * @author John Bailey
 */
public abstract class HandlerUpdateProperties implements ModelUpdateOperationHandler {
    static final String OPERATION_NAME = "update-properties";

    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        final ModelNode opAddr = operation.require(OP_ADDR);

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ModelNode model = context.getSubModel();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(OPERATION_NAME);
        compensatingOperation.get(OP_ADDR).set(opAddr);

        if (operation.hasDefined(LEVEL)) {
            apply(model, compensatingOperation, LEVEL);
            apply(operation, model, LEVEL);
        }
        if (operation.hasDefined(FORMATTER)) {
            apply(model, compensatingOperation, FORMATTER);
            apply(operation, model, FORMATTER);
        }
        if (operation.hasDefined(ENCODING)) {
            apply(model, compensatingOperation, ENCODING);
            apply(operation, model, ENCODING);
        }

        updateModel(operation, compensatingOperation, model);

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceRegistry serviceRegistry = context.getServiceRegistry();
                    final ServiceController<Handler> controller = (ServiceController<Handler>) serviceRegistry.getService(LogServices.handlerName(name));
                    if (controller != null) {
                        final Handler handler = controller.getValue();
                        if (operation.hasDefined(LEVEL)) {
                            handler.setLevel(Level.parse(operation.get(LEVEL).asString()));
                        }
                        if (operation.hasDefined(FORMATTER)) {
                            new PatternFormatterSpec(operation.get(FORMATTER).asString()).apply(handler);
                        }
                        if (operation.hasDefined(ENCODING)) {
                            try {
                                handler.setEncoding(operation.get(ENCODING).asString());
                            } catch (UnsupportedEncodingException e) {
                                throw new OperationFailedException(e, new ModelNode().set("Failed to set handler encoding."));
                            }
                        }
                        updateRuntime(operation, handler);
                    }
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }

        return new BasicOperationResult(compensatingOperation);
    }

    protected abstract void updateModel(final ModelNode operation, final ModelNode compensating, final ModelNode model);
    protected abstract void updateRuntime(final ModelNode operation, final Handler handler);

    protected void apply(ModelNode from, ModelNode to, String... attributePath) {
        if(from.get(attributePath).isDefined()) {
            to.get(attributePath).set(from.get(attributePath));
        }
    }
}
