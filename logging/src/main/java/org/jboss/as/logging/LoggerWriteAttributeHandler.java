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

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.USE_PARENT_HANDLERS;

/**
 * Date: 31.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggerWriteAttributeHandler extends AbstractWriteAttributeHandler<Logger> {
    public static final LoggerWriteAttributeHandler INSTANCE = new LoggerWriteAttributeHandler();
    private final Map<String, AttributeDefinition> attributes;

    private LoggerWriteAttributeHandler() {
        this.attributes = new HashMap<String, AttributeDefinition>();
        this.attributes.put(LEVEL.getName(), LEVEL);
        this.attributes.put(USE_PARENT_HANDLERS.getName(), USE_PARENT_HANDLERS);
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final HandbackHolder<Logger> handbackHolder) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        final ServiceController<Logger> controller = (ServiceController<Logger>) serviceRegistry.getService(LogServices.handlerName(name));
        if (controller == null) {
            return false;
        }
        // Get the logger
        final Logger logger = controller.getValue();
        if (LEVEL.getName().equals(attributeName)) {
            logger.setLevel(Level.parse(resolvedValue.asString()));
        } else if (USE_PARENT_HANDLERS.getName().equals(attributeName)) {
            logger.setUseParentHandlers(resolvedValue.asBoolean());
        }
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final Logger logger) throws OperationFailedException {
        if (LEVEL.getName().equals(attributeName)) {
            logger.setLevel(Level.parse(valueToRestore.asString()));
        } else if (USE_PARENT_HANDLERS.getName().equals(attributeName)) {
            logger.setUseParentHandlers(valueToRestore.asBoolean());
        }
    }

    @Override
    protected final void validateResolvedValue(final String name, final ModelNode value) throws OperationFailedException {
        if (attributes.containsKey(name)) {
            attributes.get(name).getValidator().validateResolvedParameter(name, value);
        } else {
            super.validateResolvedValue(name, value);
        }
    }

    @Override
    protected final void validateUnresolvedValue(final String name, final ModelNode value) throws OperationFailedException {
        if (attributes.containsKey(name)) {
            attributes.get(name).getValidator().validateParameter(name, value);
        } else {
            super.validateUnresolvedValue(name, value);
        }
    }

    /**
     * Returns a collection of attributes used for the write attribute.
     *
     * @return a collection of attributes.
     */
    public final Collection<AttributeDefinition> getAttributes() {
        return Collections.unmodifiableCollection(attributes.values());
    }
}
