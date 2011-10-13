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

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.SizeRotatingFileHandler;

import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;

/**
 * Operation responsible for updating the properties of a size based rotating log handler.
 *
 * @author John Bailey
 */
public class SizeRotatingHandlerUpdateProperties extends FlushingHandlerUpdateProperties<SizeRotatingFileHandler> {
    static final SizeRotatingHandlerUpdateProperties INSTANCE = new SizeRotatingHandlerUpdateProperties();

    private SizeRotatingHandlerUpdateProperties() {
        super(MAX_BACKUP_INDEX, ROTATE_SIZE);
    }

    @Override
    protected void updateRuntime(final ModelNode operation, final SizeRotatingFileHandler handler) throws OperationFailedException {
        super.updateRuntime(operation, handler);
        final ModelNode maxBackupIndex = MAX_BACKUP_INDEX.validateResolvedOperation(operation);
        if (maxBackupIndex.isDefined()) {
            handler.setMaxBackupIndex(maxBackupIndex.asInt());
        }

        final ModelNode rotateSizeNode = ROTATE_SIZE.validateResolvedOperation(operation);
        if (rotateSizeNode.isDefined()) {
            long rotateSize;
            try {
                rotateSize = LoggingSubsystemParser.parseSize(rotateSizeNode.asString());
            } catch (Throwable t) {
                throw new OperationFailedException(new ModelNode().set(t.getLocalizedMessage()));
            }
            handler.setRotateSize(rotateSize);
        }
    }
}
