/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;

import java.util.List;
import java.util.logging.Handler;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.logging.handlers.FlushingHandlerAddProperties;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
public class SizeRotatingFileHandlerAdd extends FlushingHandlerAddProperties<SizeRotatingFileHandlerService> {

    public static final SizeRotatingFileHandlerAdd INSTANCE = new SizeRotatingFileHandlerAdd();

    private SizeRotatingFileHandlerAdd() {
        super(APPEND, FILE, MAX_BACKUP_INDEX, ROTATE_SIZE);
    }

    @Override
    protected SizeRotatingFileHandlerService createHandlerService(OperationContext context, final ModelNode model) throws OperationFailedException {
        return new SizeRotatingFileHandlerService();
    }

    @Override
    protected void updateRuntime(final OperationContext context, final ServiceBuilder<Handler> serviceBuilder, final String name, final SizeRotatingFileHandlerService service, final ModelNode model, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        super.updateRuntime(context, serviceBuilder, name, service, model, newControllers);
        final ModelNode append = APPEND.resolveModelAttribute(context, model);
        if (append.isDefined()) {
            service.setAppend(append.asBoolean());
        }
        final ModelNode file = FILE.resolveModelAttribute(context, model);
        if (file.isDefined()) {
            newControllers.add(FileHandlers.addFile(context, serviceBuilder, service, file, name));
        }
        final ModelNode maxBackupIndex = MAX_BACKUP_INDEX.resolveModelAttribute(context, model);
        if (maxBackupIndex.isDefined()) {
            service.setMaxBackupIndex(maxBackupIndex.asInt());
        }

        final ModelNode rotateSizeNode = ROTATE_SIZE.resolveModelAttribute(context, model);
        if (rotateSizeNode.isDefined()) {
            service.setRotateSize(ModelParser.parseSize(rotateSizeNode));
        }
    }
}
