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
import org.jboss.logmanager.handlers.FileHandler;

import static org.jboss.as.logging.CommonAttributes.APPEND;

/**
 * Operation responsible for updating the properties of a file logging handler.
 *
 * @author John Bailey
 */
public class FileHandlerUpdateProperties extends FlushingHandlerUpdateProperties<FileHandler> {
    static final FileHandlerUpdateProperties INSTANCE = new FileHandlerUpdateProperties();

    private FileHandlerUpdateProperties() {
        super(APPEND);
        // TODO (jrp) consider implementing FILE as well
    }

    @Override
    protected void updateRuntime(final ModelNode operation, final FileHandler handler) throws OperationFailedException {
        super.updateRuntime(operation, handler);
        final ModelNode append = APPEND.validateResolvedOperation(operation);
        if (append.isDefined()) {
            handler.setAppend(append.asBoolean());
        }

        // TODO (jrp) consider implementing FILE as well
        /** final ServiceTarget serviceTarget = context.getServiceTarget();
         final ModelNode file = FILE.validateResolvedOperation(model);
         if (file.isDefined()) {
         final HandlerFileService fileService = new HandlerFileService(PATH.validateOperation(file).asString());
         final ServiceBuilder<?> fileBuilder = serviceTarget.addService(LogServices.handlerFileName(name), fileService);
         final ModelNode relativeTo = RELATIVE_TO.validateResolvedOperation(file);
         if (relativeTo.isDefined()) {
         fileBuilder.addDependency(AbstractPathService.pathNameOf(relativeTo.asString()), String.class, fileService.getRelativeToInjector());
         }
         fileBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
         serviceBuilder.addDependency(LogServices.handlerFileName(name), String.class, service.getFileNameInjector());
         } **/
    }
}
