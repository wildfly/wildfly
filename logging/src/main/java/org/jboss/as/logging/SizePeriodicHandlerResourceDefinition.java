/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.logging.CommonAttributes.APPEND;
import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.logmanager.handlers.SizeRotatingFileHandler;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
class SizePeriodicHandlerResourceDefinition extends AbstractFileHandlerDefinition {

    static final AttributeDefinition[] ATTRIBUTES = appendDefaultWritableAttributes(AUTOFLUSH, APPEND, FILE, MAX_BACKUP_INDEX, ROTATE_SIZE);

    static final SizePeriodicHandlerResourceDefinition INSTANCE = new SizePeriodicHandlerResourceDefinition();

    private SizePeriodicHandlerResourceDefinition() {
        super(LoggingExtension.SIZE_ROTATING_HANDLER_PATH,
                CommonAttributes.SIZE_ROTATING_FILE_HANDLER,
                new HandlerOperations.HandlerAddOperationStepHandler(SizeRotatingFileHandler.class, ATTRIBUTES, FILE, APPEND),
                ATTRIBUTES);
    }


}
