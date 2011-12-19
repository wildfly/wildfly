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

import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.LEVEL;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.logging.LoggingExtension;
import org.jboss.as.logging.util.LogServices;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
public class RootLoggerAdd extends AbstractAddStepHandler {

    public static final RootLoggerAdd INSTANCE = new RootLoggerAdd();

    public static final String OPERATION_NAME = "add";
    public static final String LEGACY_OPERATION_NAME = "set-root-logger";

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        LEVEL.validateAndSet(operation, model);
        FILTER.validateAndSet(operation, model);
        HANDLERS.validateAndSet(operation, model);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(LoggingExtension.rootLoggerPath);
        final String name = address.getLastElement().getValue();
        final ModelNode level = LEVEL.resolveModelAttribute(context, model);
        final ModelNode handlers = HANDLERS.resolveModelAttribute(context, model);
        final ModelNode filter = FILTER.resolveModelAttribute(context, model);
        final ServiceTarget target = context.getServiceTarget();

        final RootLoggerService service = new RootLoggerService();
        if (level.isDefined()) service.setLevel(ModelParser.parseLevel(level));
        if (filter.isDefined()) service.setFilter(ModelParser.parseFilter(context, filter));

        ServiceBuilder builder = target.addService(LogServices.loggerName(name), service).setInitialMode(ServiceController.Mode.ACTIVE);
        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }
        ServiceController<?> controller = builder.install();
        if (newControllers != null) {
            newControllers.add(controller);
        }

        // install logger handler services
        if (handlers.isDefined()) {
            newControllers.addAll(LoggerAssignHandler.installHandlers(target, name, handlers, verificationHandler));
        }
    }
}

