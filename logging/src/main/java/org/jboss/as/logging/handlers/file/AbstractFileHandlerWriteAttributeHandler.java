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

package org.jboss.as.logging.handlers.file;

import static org.jboss.as.logging.CommonAttributes.APPEND;
import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.FILE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.logging.handlers.AbstractLogHandlerWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.FileHandler;

/**
 * Date: 12.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractFileHandlerWriteAttributeHandler<T extends FileHandler> extends AbstractLogHandlerWriteAttributeHandler<T> {

    protected AbstractFileHandlerWriteAttributeHandler(final AttributeDefinition... attributes) {
        super(joinUnique(attributes, APPEND, AUTOFLUSH, FILE));
    }

    @Override
    protected boolean doApplyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final String handlerName, final T handler) throws OperationFailedException {
        boolean requiresRestart = false;
        if (APPEND.getName().equals(attributeName)) {
            handler.setAppend(resolvedValue.asBoolean());
            return true;
        } else if (AUTOFLUSH.getName().equals(attributeName)) {
            handler.setAutoFlush(resolvedValue.asBoolean());
        } else if (FILE.getName().equals(attributeName)) {
            requiresRestart = FileHandlers.changeFile(context, currentValue, resolvedValue, handlerName);
        }
        return requiresRestart;
    }

    @Override
    protected void doRevertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final String handlerName, final T handler) throws OperationFailedException {
        if (APPEND.getName().equals(attributeName)) {
            handler.setAppend(valueToRestore.asBoolean());
        } else if (AUTOFLUSH.getName().equals(attributeName)) {
            handler.setAutoFlush(valueToRestore.asBoolean());
        } else if (FILE.getName().equals(attributeName)) {
            FileHandlers.revertFileChange(context, valueToRestore, handlerName);
        }
    }
}
