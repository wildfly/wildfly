/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.services.path;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.services.path.PathResourceDefinition.READ_ONLY;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager.Event;
import org.jboss.as.controller.services.path.PathManagerService.PathEventContextImpl;
import org.jboss.dmr.ModelNode;

/**
 * {@link OperationStepHandler} for the write-attribute operation for a path resource.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class PathWriteAttributeHandler extends AbstractWriteAttributeHandler<PathWriteAttributeHandler.PathUpdate> {

    private final PathManagerService pathManager;
    private final boolean services;


    PathWriteAttributeHandler(final PathManagerService pathManager, final SimpleAttributeDefinition definition, final boolean services) {
        super(definition);
        this.pathManager = pathManager;
        this.services = services;
    }

    @Override
    protected void validateUpdatedModel(OperationContext context, ModelNode operation, Resource model) throws OperationFailedException {
        // Guard against updates to read-only paths
        final String pathName = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();
        if (model.getModel().get(READ_ONLY.getName()).asBoolean(false)) {
            throw ControllerMessages.MESSAGES.cannotModifyReadOnlyPath(pathName);
        }
        if (services) {
            final PathEntry pathEntry = pathManager.getPathEntry(pathName);
            if (pathEntry.isReadOnly()) {
                throw MESSAGES.pathEntryIsReadOnly(operation.require(OP_ADDR).asString());
            }
        }
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return services;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<PathUpdate> handbackHolder) throws OperationFailedException {
        final String pathName = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();
        final PathEntry pathEntry = pathManager.getPathEntry(pathName);
        final PathEntry backup = new PathEntry(pathEntry);

        final PathEventContextImpl pathEventContext = pathManager.checkRestartRequired(context, pathName, Event.UPDATED);
        if (pathEventContext.isInstallServices()) {
            final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
            context.addStep(verificationHandler, Stage.VERIFY);
            if (attributeName.equals(PATH)) {
                String pathVal = resolvedValue.asString();
                pathManager.changePath(pathName, pathVal);
                pathManager.changePathServices(context, pathName, pathVal, verificationHandler);
            } else if (attributeName.equals(RELATIVE_TO)) {
                String relToVal = resolvedValue.isDefined() ?  resolvedValue.asString() : null;
                pathManager.changeRelativePath( pathName, relToVal, true);
                pathManager.changeRelativePathServices(context, pathName, relToVal, verificationHandler);
            }
        }

        handbackHolder.setHandback(new PathUpdate(backup, pathEventContext));

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, PathUpdate handback) throws OperationFailedException {
        final String pathName = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();
        final PathEntry backup = handback.backup;
        final PathEventContextImpl pathEventContext = handback.context;
        if (pathEventContext.isInstallServices()) {
            if (attributeName.equals(PATH)) {
                pathManager.changePath(pathName, backup.getPath());
                pathManager.changePathServices(context, pathName, valueToRestore.asString(), null);
            } else if (attributeName.equals(RELATIVE_TO)) {
                try {
                    pathManager.changeRelativePath(pathName, backup.getRelativeTo(), false);
                } catch (OperationFailedException e) {
                    //Should not happen since false passed in for the 'check' parameter
                    throw new RuntimeException(e);
                }
                pathManager.changeRelativePathServices(context, pathName, valueToRestore.isDefined() ?  valueToRestore.asString() : null, null);
            }
        } else {
            pathEventContext.revert();
        }
    }

    static class PathUpdate {
        private final PathEntry backup;
        private final PathEventContextImpl context;

        private PathUpdate(PathEntry backup, PathEventContextImpl context) {
            this.backup = backup;
            this.context = context;
        }
    }
}
