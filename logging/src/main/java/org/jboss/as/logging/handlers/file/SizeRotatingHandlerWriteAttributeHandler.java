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

import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.SizeRotatingFileHandler;

/**
 * Date: 13.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class SizeRotatingHandlerWriteAttributeHandler extends AbstractFileHandlerWriteAttributeHandler<SizeRotatingFileHandlerService> {
    public static final SizeRotatingHandlerWriteAttributeHandler INSTANCE = new SizeRotatingHandlerWriteAttributeHandler();

    private SizeRotatingHandlerWriteAttributeHandler() {
        super(MAX_BACKUP_INDEX, ROTATE_SIZE);
    }

    @Override
    protected boolean doApplyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final String handlerName, final SizeRotatingFileHandlerService handlerService) throws OperationFailedException {
        boolean result = super.doApplyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, handlerName, handlerService);
        if (MAX_BACKUP_INDEX.getName().equals(attributeName)) {
            handlerService.setMaxBackupIndex(resolvedValue.asInt());
            result = false;
        } else if (ROTATE_SIZE.getName().equals(attributeName)) {
            handlerService.setRotateSize(ModelParser.parseSize(resolvedValue));
            result = false;
        }
        return result;
    }

    @Override
    protected void doRevertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final String handlerName, final SizeRotatingFileHandlerService handlerService) throws OperationFailedException {
        super.doRevertUpdateToRuntime(context, operation, attributeName, valueToRestore, valueToRevert, handlerName, handlerService);
        if (MAX_BACKUP_INDEX.getName().equals(attributeName)) {
            handlerService.setMaxBackupIndex(valueToRestore.asInt());
        } else if (ROTATE_SIZE.getName().equals(attributeName)) {
            handlerService.setRotateSize(ModelParser.parseSize(valueToRestore));
        }
    }
}
