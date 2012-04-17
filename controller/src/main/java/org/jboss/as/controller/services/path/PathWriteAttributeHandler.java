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

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager.Event;
import org.jboss.as.controller.services.path.PathManagerService.PathEventContextImpl;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class PathWriteAttributeHandler extends WriteAttributeHandlers.WriteAttributeOperationHandler {

    private final PathManagerService pathManager;
    private final boolean services;


    PathWriteAttributeHandler(final PathManagerService pathManager, final SimpleAttributeDefinition definition, final boolean services) {
        super(definition.getValidator());
        this.pathManager = pathManager;
        this.services = services;
    }

    @Override
    protected void modelChanged(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode newValue,
            final ModelNode currentValue) throws OperationFailedException {

        final String pathName = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();
        final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        if (model.get(READ_ONLY.getName()).asBoolean(false)) {
            throw ControllerMessages.MESSAGES.cannotModifyReadOnlyPath(pathName);
        }

        if (services) {
            final PathEntry pathEntry = pathManager.getPathEntry(pathName);
            if (pathEntry.isReadOnly()) {
                throw MESSAGES.pathEntryIsReadOnly(operation.require(OP_ADDR).asString());
            }

            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final PathEntry backup = new PathEntry(pathEntry);

                    final PathEventContextImpl pathEventContext = pathManager.checkRestartRequired(context, pathName, Event.UPDATED);
                    if (pathEventContext.isInstallServices()) {
                        if (attributeName.equals(PATH)) {
                            pathManager.changePath(pathName, newValue.asString());
                            pathManager.changePathServices(context, pathName, newValue.asString());
                        } else if (attributeName.equals(RELATIVE_TO)) {
                            pathManager.changeRelativePath( pathName, newValue.isDefined() ?  newValue.asString() : null, true);
                            pathManager.changeRelativePathServices(context, pathName, newValue.isDefined() ?  newValue.asString() : null);
                        }
                    }

                    context.completeStep(new OperationContext.RollbackHandler() {
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            if (pathEventContext.isInstallServices()) {
                                if (attributeName.equals(PATH)) {
                                    pathManager.changePath(pathName, backup.getPath());
                                    pathManager.changePathServices(context, pathName, currentValue.asString());
                                } else if (attributeName.equals(RELATIVE_TO)) {
                                    try {
                                        pathManager.changeRelativePath(pathName, backup.getRelativeTo(), false);
                                    } catch (OperationFailedException e) {
                                        //Should not happen since false passed in for the 'check' parameter
                                        throw new RuntimeException(e);
                                    }
                                    pathManager.changeRelativePathServices(context, pathName, currentValue.isDefined() ?  currentValue.asString() : null);
                                }
                            } else {
                                pathEventContext.revert();
                            }
                        }
                    });
                }
            }, Stage.RUNTIME);
        }
        context.completeStep();
    }
}
