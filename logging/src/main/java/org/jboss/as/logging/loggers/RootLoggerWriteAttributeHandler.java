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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.LEVEL;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.logging.util.LogServices;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Date: 31.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class RootLoggerWriteAttributeHandler extends AbstractLoggerWriteAttributeHandler {
    public static final RootLoggerWriteAttributeHandler INSTANCE = new RootLoggerWriteAttributeHandler();

    private RootLoggerWriteAttributeHandler() {
        super(LEVEL, FILTER, HANDLERS);
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final HandbackHolder<Logger> handbackHolder) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        @SuppressWarnings("unchecked")
        final ServiceController<Logger> controller = (ServiceController<Logger>) serviceRegistry.getService(LogServices.handlerName(name));
        if (controller == null) {
            return false;
        }
        // Get the logger
        final Logger logger = controller.getValue();
        if (LEVEL.getName().equals(attributeName)) {
            logger.setLevel(ModelParser.parseLevel(resolvedValue));
        } else if (FILTER.getName().equals(attributeName)) {
            logger.setFilter(ModelParser.parseFilter(context, resolvedValue));
        } else if (HANDLERS.getName().equals(attributeName)) {
            // Remove all handlers
            LoggerUnassignHandler.removeHandlers(HANDLERS, currentValue, context, name);
            // Add the new handlers
            LoggerAssignHandler.addHandlers(HANDLERS.resolveModelAttribute(context, resolvedValue), context, name, null);
        }
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final Logger logger) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        if (LEVEL.getName().equals(attributeName)) {
            logger.setLevel(ModelParser.parseLevel(valueToRestore));
        } else if (FILTER.getName().equals(attributeName)) {
            logger.setFilter(ModelParser.parseFilter(context, valueToRestore));
        } else if (HANDLERS.getName().equals(attributeName)) {
            // Remove the new handlers
            LoggerUnassignHandler.removeHandlers(HANDLERS, valueToRevert, context, name);
            // Re-add the old handlers
            LoggerAssignHandler.addHandlers(HANDLERS.resolveModelAttribute(context, valueToRestore), context, name, null);
        }
    }
}
