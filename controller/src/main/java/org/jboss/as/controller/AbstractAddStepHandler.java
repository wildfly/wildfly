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

import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Base class for {@link OperationStepHandler} implementations that add managed resource.
 *
 * @author John Bailey
 */
public abstract class AbstractAddStepHandler implements OperationStepHandler {

    /** {@inheritDoc */
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        populateModel(context, operation, resource);
        final ModelNode model = resource.getModel();

        if (requiresRuntime(context)) {
            context.addStep(new OperationStepHandler() {
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
                    final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    performRuntime(context, operation, model, verificationHandler, controllers);

                    if(requiresRuntimeVerification()) {
                        context.addStep(verificationHandler, OperationContext.Stage.VERIFY);
                    }

                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            rollbackRuntime(context, operation, model, controllers);
                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    /**
     * Populate the given resource in the persistent configuration model based on the values in the given operation.
     * <p>
     * This default implementation simply calls {@link #populateModel(ModelNode, Resource)}.
     * </p>
     *
     * @param context the operation context
     * @param operation the operation
     * @param resource the resource that corresponds to the address of {@code operation}
     *
     * @throws OperationFailedException if {@code operation} is invalid or populating the model otherwise fails
     */
    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws  OperationFailedException {
        populateModel(operation, resource);
    }

    /**
     * Populate the given resource in the persistent configuration model based on the values in the given operation.
     *
     * @param operation the operation
     * @param resource the resource that corresponds to the address of {@code operation}
     *
     * @throws OperationFailedException if {@code operation} is invalid or populating the model otherwise fails
     */
    protected void populateModel(final ModelNode operation, final Resource resource) throws  OperationFailedException {
        populateModel(operation, resource.getModel());
    }

    /**
     * Populate the given node in the persistent configuration model based on the values in the given operation.
     *
     * @param operation the operation
     * @param model persistent configuration model node that corresponds to the address of {@code operation}
     *
     * @throws OperationFailedException if {@code operation} is invalid or populating the model otherwise fails
     */
    protected abstract void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException;

    /**
     * Gets whether {@link #performRuntime(OperationContext, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, ServiceVerificationHandler, java.util.List)}}
     * should be called. This default implementation always returns {@code true}. Subclasses that perform no runtime
     * update could override and return {@code false}.
     *
     * @param context operation context
     * @return {@code true} if {@code performRuntime} should be invoked; {@code false} otherwise.
     */
    protected boolean requiresRuntime(OperationContext context) {
        return context.isNormalServer();
    }

    /**
     * Gets whether the {@link ServiceVerificationHandler} parameter passed to
     * {@link #performRuntime(OperationContext, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, ServiceVerificationHandler, java.util.List)}
     * should be added to the operation context as a step.
     * <p>
     * This default implementation always returns {@code true}.
     * </p>
     *
     * @return  {@code true} if the service verification step should be added; {@code false} if it's not necessary.
     */
    protected boolean requiresRuntimeVerification() {
        return true;
    }

    /**
     * Make any runtime changes necessary to effect the changes indicated by the given {@code operation}. Executes
     * after {@link #populateModel(org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode)}, so the given {@code model}
     * parameter will reflect any changes made in that method.
     * <p>
     * This default implementation does nothing.
     * </p>
     *
     * @param context  the operation context
     * @param operation the operation being executed
     * @param model persistent configuration model node that corresponds to the address of {@code operation}
     * @param verificationHandler step handler that can be added as a listener to any new services installed in order to
     *                            validate the services installed correctly during the
     *                            {@link OperationContext.Stage#VERIFY VERIFY stage}
     * @param newControllers holder for the {@link ServiceController} for any new services installed by the method. The
     *                       method should add the {@code ServiceController} for any new services to this list. If the
     *                       overall operation needs to be rolled back, the list will be used in
     *                       {@link #rollbackRuntime(OperationContext, ModelNode, ModelNode, java.util.List)}  to automatically removed
     *                       the newly added services
     * @throws OperationFailedException if {@code operation} is invalid or updating the runtime otherwise fails
     */
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
    }

    /**
     * Rollback runtime changes made in {@link #performRuntime(OperationContext, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, ServiceVerificationHandler, java.util.List)}.
     * <p>
     * This default implementation removes all services in the given list of {@code controllers}. The contents of
     * {@code controllers} is the same as what was in the {@code newControllers} parameter passed to {@code performRuntime()}
     * when that method returned.
     * </p>
     * @param context the operation context
     * @param operation the operation being executed
     * @param model persistent configuration model node that corresponds to the address of {@code operation}
     * @param controllers  holder for the {@link ServiceController} for any new services installed by
     *                     {@link #performRuntime(OperationContext, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, ServiceVerificationHandler, java.util.List)}
     */
    protected void rollbackRuntime(OperationContext context, final ModelNode operation, final ModelNode model, List<ServiceController<?>> controllers) {
        for(ServiceController<?> controller : controllers) {
            context.removeService(controller.getName());
        }
    }
}
