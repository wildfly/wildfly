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

import org.jboss.as.controller.AbstractModelUpdateHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Handler;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

/**
 * Parent operation responsible for updating the common attributes of logging handlers.
 *
 * @author John Bailey
 */
public abstract class HandlerUpdateProperties extends AbstractModelUpdateHandler {
    static final String OPERATION_NAME = "update-properties";

    @Override
    protected void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        LEVEL.validateAndSet(operation, model);
        FORMATTER.validateAndSet(operation, model);
        ENCODING.validateAndSet(operation, model);
    }

    @Override
    protected final void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                        final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        final ServiceController<Handler> controller = (ServiceController<Handler>) serviceRegistry.getService(LogServices.handlerName(name));
        if (controller != null) {
            final Handler handler = controller.getValue();
            final ModelNode level = LEVEL.validateResolvedOperation(model);
            final ModelNode formatter = FORMATTER.validateResolvedOperation(model);
            final ModelNode encoding = ENCODING.validateResolvedOperation(model);

            if (level.isDefined()) {
                handler.setLevel(java.util.logging.Level.parse(level.asString()));
            }

            if (formatter.isDefined()) {
                AbstractFormatterSpec.fromModelNode(model).apply(handler);
            }

            if (encoding.isDefined()) {
                try {
                    handler.setEncoding(encoding.asString());
                } catch (UnsupportedEncodingException e) {
                    throw new OperationFailedException(e, new ModelNode().set(MESSAGES.failedToSetHandlerEncoding()));
                }
            }
            updateRuntime(model, handler);
        }
    }

    protected abstract void updateRuntime(final ModelNode operation, final Handler handler) throws OperationFailedException;
}
