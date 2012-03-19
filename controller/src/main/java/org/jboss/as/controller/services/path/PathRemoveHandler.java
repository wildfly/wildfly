/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.services.path;


import static org.jboss.as.controller.ControllerLogger.MGMT_OP_LOGGER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.services.path.PathResourceDefinition.PATH_SPECIFIED;
import static org.jboss.as.controller.services.path.PathResourceDefinition.READ_ONLY;
import static org.jboss.as.controller.services.path.PathResourceDefinition.RELATIVE_TO;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager.Event;
import org.jboss.as.controller.services.path.PathManagerService.PathEventContextImpl;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handler for the path resource remove operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class PathRemoveHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = REMOVE;

    private final boolean services;

    private final PathManagerService pathManager;

    public static ModelNode getRemovePathOperation(ModelNode address) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        return op;
    }

    /**
     * Create the PathRemoveHandler
     */
    protected PathRemoveHandler(final PathManagerService pathManager, final boolean services) {
        this.pathManager = pathManager;
        this.services = services;
    }

    static PathRemoveHandler createNamedInstance(final PathManagerService pathManager) {
        return new PathRemoveHandler(pathManager, false);
    }

    static PathRemoveHandler createSpecifiedInstance(final PathManagerService pathManager) {
        return new PathRemoveHandler(pathManager, true);
    }

    static PathRemoveHandler createSpecifiedNoServicesInstance(final PathManagerService pathManager) {
        return new PathRemoveHandler(pathManager, true);
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        if (model.get(READ_ONLY.getName()).asBoolean(false)) {
            throw ControllerMessages.MESSAGES.cannotRemoveReadOnlyPath(name);
        }

        context.removeResource(PathAddress.EMPTY_ADDRESS);

        if (services) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final PathEventContextImpl pathEventContext = pathManager.checkRestartRequired(context, name, Event.REMOVED);
                    if (pathEventContext.isInstallServices()) {
                        pathManager.removePathService(context, name);
                        pathManager.removePathEntry(name);
                    }

                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            try {
                                final String path = PathAddHandler.getPathValue(context, PATH_SPECIFIED, model);
                                final String relativeTo = PathAddHandler.getPathValue(context, RELATIVE_TO, model);
                                if (pathEventContext.isInstallServices()) {
                                    pathManager.addPathEntry(name, path, relativeTo, false);
                                    final ServiceTarget target = context.getServiceTarget();
                                    if (relativeTo == null) {
                                        pathManager.addAbsolutePathService(target, name, path, null);
                                    } else {
                                        pathManager.addRelativePathService(target, name, path, false, relativeTo, null);
                                    }
                                } else {
                                    context.revertRestartRequired();
                                }


                            } catch (Exception e) {
                                MGMT_OP_LOGGER.errorRevertingOperation(e, getClass().getSimpleName(),
                                    operation.require(ModelDescriptionConstants.OP).asString(),
                                    PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
                            }
                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }
}
