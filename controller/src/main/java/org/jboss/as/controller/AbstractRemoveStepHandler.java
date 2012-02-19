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

package org.jboss.as.controller;

import static org.jboss.as.controller.ControllerLogger.MGMT_OP_LOGGER;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Base class for handlers that remove resources.
 *
 * @author John Bailey
 */
public abstract class AbstractRemoveStepHandler implements OperationStepHandler {

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        performRemove(context, operation, model);

        if (requiresRuntime(context)) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    performRuntime(context, operation, model);

                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            try {
                                recoverServices(context, operation, model);
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

    protected void performRemove(OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        if (!requireNoChildResources() || resource.getChildTypes().isEmpty()) {
            context.removeResource(PathAddress.EMPTY_ADDRESS);
        } else {
            List<PathElement> children = getChildren(resource);
            throw ControllerMessages.MESSAGES.cannotRemoveResourceWithChildren(children);
        }
    }

    protected boolean requireNoChildResources() {
        return false;
    }

    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
    }

    protected void recoverServices(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
    }

    protected boolean requiresRuntime(OperationContext context) {
        return context.isNormalServer();
    }

    private List<PathElement> getChildren(Resource resource) {
        final List<PathElement> pes = new ArrayList<PathElement>();
        for (String childType : resource.getChildTypes()) {
            for (Resource.ResourceEntry entry : resource.getChildren(childType)) {
                pes.add(entry.getPathElement());
            }
        }
        return pes;
    }


}
