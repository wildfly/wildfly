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

package org.jboss.as.logging.loggers;

import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.ROOT_LOGGER;

import java.util.List;

import org.jboss.as.controller.AbstractModelUpdateHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.logging.util.LogServices;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Operation responsible for changing the logging level of the root logger.
 *
 * @author John Bailey
 */
public class RootLoggerLevelChange extends AbstractModelUpdateHandler {
    public static final String OPERATION_NAME = "change-root-log-level";
    public static final RootLoggerLevelChange INSTANCE = new RootLoggerLevelChange();

    @Override
    protected void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        LEVEL.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        @SuppressWarnings("unchecked")
        final ServiceController<Logger> controller = (ServiceController<Logger>) serviceRegistry.getService(LogServices.ROOT_LOGGER);
        final ModelNode level = LEVEL.resolveModelAttribute(context, model);
        if (controller != null && level.isDefined()) {
            controller.getValue().setLevel(ModelParser.parseLevel(level));
        }
    }
}
