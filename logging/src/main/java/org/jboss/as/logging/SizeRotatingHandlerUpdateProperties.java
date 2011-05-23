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

import java.util.logging.Handler;
import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;
import static org.jboss.as.logging.CommonAttributes.SUFFIX;
import static org.jboss.as.logging.SizeRotatingFileHandlerAdd.DEFAULT_ROTATE_SIZE;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.SizeRotatingFileHandler;

/**
 * Operation responsible for updating the properties of a size based rotating log handler.
 *
 * @author John Bailey
 */
public class SizeRotatingHandlerUpdateProperties extends FlushingHandlerUpdateProperties {
    static final SizeRotatingHandlerUpdateProperties INSTANCE = new SizeRotatingHandlerUpdateProperties();

    protected void updateModel(final ModelNode operation, final ModelNode model) {
        super.updateModel(operation, model);

        if (operation.hasDefined(MAX_BACKUP_INDEX)) {
            apply(operation, model, MAX_BACKUP_INDEX);
        }
        if (operation.hasDefined(ROTATE_SIZE)) {
            apply(operation, model, ROTATE_SIZE);
        }
    }

    protected void updateRuntime(final ModelNode operation, final Handler handler) {
        super.updateRuntime(operation, handler);
        if (operation.hasDefined(MAX_BACKUP_INDEX)) {
            SizeRotatingFileHandler.class.cast(handler).setMaxBackupIndex(operation.get(MAX_BACKUP_INDEX).asInt());
        }
        if (operation.hasDefined(ROTATE_SIZE)) {
            SizeRotatingFileHandler.class.cast(handler).setRotateSize(operation.get(ROTATE_SIZE).asLong(DEFAULT_ROTATE_SIZE));
        }
    }
}
