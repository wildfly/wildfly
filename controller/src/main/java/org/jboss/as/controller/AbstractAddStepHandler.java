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

import java.util.ArrayList;
import java.util.List;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author John Bailey
 */
public abstract class AbstractAddStepHandler implements NewStepHandler {

    public void execute(final NewOperationContext context, final ModelNode operation) throws OperationFailedException {
        final ModelNode model = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);
        populateModel(operation, model);

        if (requiresRuntime()) {
            if (context.getType() == NewOperationContext.Type.SERVER) {
                context.addStep(new NewStepHandler() {
                    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
                        final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
                        final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                        performRuntime(context, operation, model, verificationHandler, controllers);

                        if(requiresRuntimeVerification()) {
                            context.addStep(verificationHandler, NewOperationContext.Stage.VERIFY);
                        }

                        if (context.completeStep() == NewOperationContext.ResultAction.ROLLBACK) {
                            for(ServiceController<?> controller : controllers) {
                                context.removeService(controller.getName());
                            }
                        }
                    }
                }, NewOperationContext.Stage.RUNTIME);
            }
        }
        context.completeStep();
    }

    protected abstract void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException;

    protected void performRuntime(final NewOperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
    }

    protected boolean requiresRuntime() {
        return true;
    }

    protected boolean requiresRuntimeVerification() {
        return true;
    }
}
