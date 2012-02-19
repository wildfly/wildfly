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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.FILE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Operation responsible for changing the file attributes of file based logging handlers.
 *
 * @author John Bailey
 */
public class HandlerFileChange implements OperationStepHandler {
    public static final String OPERATION_NAME = "change-file";
    public static final HandlerFileChange INSTANCE = new HandlerFileChange();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode originalModel = resource.getModel().clone();
        final ModelNode model = resource.getModel();
        FILE.validateAndSet(operation, model);

        if (context.isNormalServer() && !context.isBooting()) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
                    final String name = address.getLastElement().getValue();
                    final ModelNode oldFile = FILE.resolveModelAttribute(context, originalModel);
                    final ModelNode file = FILE.resolveModelAttribute(context, model);
                    if (FileHandlers.changeFile(context, oldFile, file, name)) {
                        context.restartRequired();
                    }
                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        FileHandlers.revertFileChange(context, FILE.resolveModelAttribute(context, originalModel), name);
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }
}
