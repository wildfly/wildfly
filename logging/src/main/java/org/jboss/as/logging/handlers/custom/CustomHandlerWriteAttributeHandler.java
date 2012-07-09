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

package org.jboss.as.logging.handlers.custom;

import static org.jboss.as.logging.CommonAttributes.PROPERTIES;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import java.util.logging.Handler;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.logging.handlers.AbstractLogHandlerWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Date: 12.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class CustomHandlerWriteAttributeHandler extends AbstractLogHandlerWriteAttributeHandler<CustomHandlerService> {
    public static final CustomHandlerWriteAttributeHandler INSTANCE = new CustomHandlerWriteAttributeHandler();

    private CustomHandlerWriteAttributeHandler() {
        super();
    }

    @Override
    protected boolean doApplyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final String handlerName, final CustomHandlerService handlerService) throws OperationFailedException {
        if (PROPERTIES.equals(attributeName)) {
            if (resolvedValue.getType() != ModelType.LIST) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidType(PROPERTIES, ModelType.LIST, resolvedValue.getType())));
            }
            handlerService.setProperties(resolvedValue.asPropertyList());
        }
        return false;
    }

    @Override
    protected void doRevertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final String handlerName, final CustomHandlerService handlerService) throws OperationFailedException {
        if (PROPERTIES.equals(attributeName)) {
            if (valueToRestore.getType() != ModelType.LIST) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidType(PROPERTIES, ModelType.LIST, valueToRestore.getType())));
            }
            handlerService.setProperties(valueToRestore.asPropertyList());
        }
    }
}
