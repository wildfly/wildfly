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
import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.logging.handlers.HandlerUpdateProperties;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.SizeRotatingFileHandler;

/**
 * Operation responsible for updating the properties of a size based rotating log handler.
 *
 * @author John Bailey
 */
public class SizeRotatingHandlerUpdateProperties extends HandlerUpdateProperties<SizeRotatingFileHandler> {
    public static final SizeRotatingHandlerUpdateProperties INSTANCE = new SizeRotatingHandlerUpdateProperties();

    private SizeRotatingHandlerUpdateProperties() {
        super(APPEND, AUTOFLUSH, MAX_BACKUP_INDEX, ROTATE_SIZE);
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final String handlerName, final ModelNode model,
                                           final ModelNode originalModel, final SizeRotatingFileHandler handler) throws OperationFailedException {
        final ModelNode autoflush = AUTOFLUSH.resolveModelAttribute(context, model);
        if (autoflush.isDefined()) {
            handler.setAutoFlush(autoflush.asBoolean());
        }
        final ModelNode append = APPEND.resolveModelAttribute(context, model);
        if (append.isDefined()) {
            handler.setAppend(append.asBoolean());
        }
        final ModelNode maxBackupIndex = MAX_BACKUP_INDEX.resolveModelAttribute(context, model);
        if (maxBackupIndex.isDefined()) {
            handler.setMaxBackupIndex(maxBackupIndex.asInt());
        }

        final ModelNode rotateSizeNode = ROTATE_SIZE.resolveModelAttribute(context, model);
        if (rotateSizeNode.isDefined()) {
            handler.setRotateSize(ModelParser.parseSize(rotateSizeNode));
        }
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final String handlerName, final ModelNode model,
                                         final ModelNode originalModel, final SizeRotatingFileHandler handler) throws OperationFailedException {
        final ModelNode autoflush = AUTOFLUSH.resolveModelAttribute(context, originalModel);
        if (autoflush.isDefined()) {
            handler.setAutoFlush(autoflush.asBoolean());
        }
        final ModelNode append = APPEND.resolveModelAttribute(context, originalModel);
        if (append.isDefined()) {
            handler.setAppend(append.asBoolean());
        }
        final ModelNode maxBackupIndex = MAX_BACKUP_INDEX.resolveModelAttribute(context, originalModel);
        if (maxBackupIndex.isDefined()) {
            handler.setMaxBackupIndex(maxBackupIndex.asInt());
        }

        final ModelNode rotateSizeNode = ROTATE_SIZE.resolveModelAttribute(context, originalModel);
        if (rotateSizeNode.isDefined()) {
            handler.setRotateSize(ModelParser.parseSize(rotateSizeNode));
        }
    }
}
