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

import org.jboss.dmr.ModelNode;

/**
 * @author John Bailey
 */
public abstract class AbstractRemoveStepHandler implements NewStepHandler {

    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);

        performRemove(context, operation, model);

        if (requiresRuntime()) {
            if (requiresRuntime()) {
                if (context.getType() == NewOperationContext.Type.SERVER) {
                    context.addStep(new NewStepHandler() {
                        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
                            performRuntime(context, operation, model
                            );

                            if (context.completeStep() == NewOperationContext.ResultAction.ROLLBACK) {
                                recoverServices(context, operation, model);
                            }
                        }
                    }, NewOperationContext.Stage.RUNTIME);
                }
            }
        }
        context.completeStep();
    }

    protected void performRemove(NewOperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        context.removeModel(PathAddress.EMPTY_ADDRESS);
    }

    protected void performRuntime(final NewOperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
    }

    protected void recoverServices(final NewOperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
    }

    protected boolean requiresRuntime() {
        return true;
    }


}
