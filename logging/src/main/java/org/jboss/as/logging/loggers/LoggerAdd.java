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

package org.jboss.as.logging.loggers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.CATEGORY;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.USE_PARENT_HANDLERS;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.logging.util.LogServices;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
public class LoggerAdd extends AbstractAddStepHandler {

    public static final LoggerAdd INSTANCE = new LoggerAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        CATEGORY.validateAndSet(operation, model);
        LEVEL.validateAndSet(operation, model);
        HANDLERS.validateAndSet(operation, model);
        USE_PARENT_HANDLERS.validateAndSet(operation, model);
        FILTER.validateAndSet(operation, model);
        if (!operation.hasDefined(CATEGORY.getName())) {
            model.get(CATEGORY.getName()).set(PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue());
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ModelNode level = LEVEL.resolveModelAttribute(context, model);
        final ModelNode useParentHandlers = USE_PARENT_HANDLERS.resolveModelAttribute(context, model);
        final ModelNode filter = FILTER.resolveModelAttribute(context, model);
        final ModelNode category = CATEGORY.resolveModelAttribute(context, model);
        final String loggerName = category.isDefined() ? category.asString() : name;

        final ServiceTarget target = context.getServiceTarget();
        try {
            // Install logger service
            final LoggerService service = new LoggerService(loggerName);
            if (level.isDefined()) service.setLevel(ModelParser.parseLevel(level));
            if (useParentHandlers.isDefined()) service.setUseParentHandlers(useParentHandlers.asBoolean());
            if (filter.isDefined()) service.setFilter(ModelParser.parseFilter(context, filter));
            newControllers.add(target.addService(LogServices.loggerName(name), service)
                    .addListener(verificationHandler)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install());
        } catch (Throwable t) {
            throw new OperationFailedException(new ModelNode().set(t.getLocalizedMessage()));
        }
        try {
            // install logger handler services
            final ModelNode handlers = HANDLERS.resolveModelAttribute(context, model);
            if (handlers.isDefined()) {
                newControllers.addAll(LoggerAssignHandler.installHandlers(target, name, handlers, verificationHandler));
            }
        } catch (Throwable t) {
            throw new OperationFailedException(new ModelNode().set(t.getLocalizedMessage()));
        }
    }
}
