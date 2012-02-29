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

import static org.jboss.as.logging.CommonAttributes.PATH;
import static org.jboss.as.logging.CommonAttributes.RELATIVE_TO;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import java.io.FileNotFoundException;
import java.util.logging.Handler;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.services.path.AbstractPathService;
import org.jboss.as.logging.util.LogServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * Date: 23.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class FileHandlers {

    static void addFile(final OperationContext context, final ServiceBuilder<Handler> serviceBuilder, final AbstractFileHandlerService service, final ModelNode file, final String name) throws OperationFailedException {
        if (file.isDefined()) {
            final ModelNode path = PATH.resolveModelAttribute(context, file);
            final ModelNode relativeTo = RELATIVE_TO.resolveModelAttribute(context, file);
            final ServiceName serviceName = LogServices.handlerFileName(name);

            // Retrieve the current service
            final ServiceTarget serviceTarget = context.getServiceTarget();
            final HandlerFileService fileService = new HandlerFileService(path.asString());
            final ServiceBuilder<?> fileBuilder = serviceTarget.addService(serviceName, fileService);
            // Add the relative path dependency
            if (relativeTo.isDefined()) {
                fileBuilder.addDependency(AbstractPathService.pathNameOf(relativeTo.asString()), String.class, fileService.getRelativeToInjector());
            }
            fileBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
            serviceBuilder.addDependency(LogServices.handlerFileName(name), String.class, service.getFileNameInjector());
        }
    }

    static boolean changeFile(final OperationContext context, final ModelNode oldFile, final ModelNode newFile, final String name) throws OperationFailedException {
        boolean requiresRestart = false;
        if (newFile.isDefined()) {
            final ModelNode path = PATH.resolveModelAttribute(context, newFile);
            final ModelNode relativeTo = RELATIVE_TO.resolveModelAttribute(context, newFile);
            final ModelNode currentRelativeTo = RELATIVE_TO.resolveModelAttribute(context, oldFile);
            // If relative-to is defined, no need to continue.
            if (relativeTo.isDefined() && !currentRelativeTo.equals(relativeTo) && !AbstractPathService.isAbsoluteUnixOrWindowsPath(path.asString())) {
                requiresRestart = true;
            } else {
                final ServiceName serviceName = LogServices.handlerFileName(name);

                // Retrieve the current service
                final ServiceRegistry registry = context.getServiceRegistry(true);
                final ServiceController<?> fileController = registry.getService(serviceName);
                if (fileController == null) {
                    throw new OperationFailedException(new ModelNode().set(MESSAGES.serviceNotFound(serviceName)));
                }
                final HandlerFileService fileService = (HandlerFileService) fileController.getService();
                fileService.setPath(path.asString());

                // Find the handler and set the new file
                @SuppressWarnings("unchecked")
                final ServiceController<?> handlerController = registry.getService(LogServices.handlerName(name));
                final AbstractFileHandlerService handlerService = (AbstractFileHandlerService) handlerController.getService();
                final String fileName = fileService.getValue();
                try {
                    handlerService.setFile(fileName);
                } catch (FileNotFoundException e) {
                    throw new OperationFailedException(e, new ModelNode().set(MESSAGES.fileNotFound(fileName)));
                }
            }
        }
        return requiresRestart;
    }

    static void revertFileChange(final OperationContext context, final ModelNode file, final String name) throws OperationFailedException {
        if (file.isDefined()) {
            final ModelNode path = PATH.resolveModelAttribute(context, file);
            final ServiceName serviceName = LogServices.handlerFileName(name);

            // Retrieve the current service
            final ServiceRegistry registry = context.getServiceRegistry(true);
            final ServiceController<?> fileController = registry.getService(serviceName);
            if (fileController == null) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.serviceNotFound(serviceName)));
            }
            final HandlerFileService fileService = (HandlerFileService) fileController.getService();
            fileService.setPath(path.asString());

            // Find the handler and set the new file
            @SuppressWarnings("unchecked")
            final ServiceController<?> handlerController = registry.getService(LogServices.handlerName(name));
            final AbstractFileHandlerService handlerService = (AbstractFileHandlerService) handlerController.getService();
            final String fileName = fileService.getValue();
            try {
                handlerService.setFile(fileName);
            } catch (FileNotFoundException e) {
                throw new OperationFailedException(e, new ModelNode().set(MESSAGES.fileNotFound(fileName)));
            }
        }
    }
}
