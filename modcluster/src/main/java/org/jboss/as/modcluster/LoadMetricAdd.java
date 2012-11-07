/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.modcluster;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.List;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class LoadMetricAdd extends AbstractAddStepHandler {


    static final LoadMetricAdd INSTANCE = new LoadMetricAdd();

    private LoadMetricAdd() {

    }

    /**
     * Populate the given node in the persistent configuration model based on the values in the given operation.
     *
     * @param operation the operation
     * @param model     persistent configuration model node that corresponds to the address of {@code operation}
     * @throws org.jboss.as.controller.OperationFailedException
     *          if {@code operation} is invalid or populating the model otherwise fails
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition def : LoadMetricDefinition.ATTRIBUTES) {
            def.validateAndSet(operation, model);
        }
    }

    /**
     * Make any runtime changes necessary to effect the changes indicated by the given {@code operation}. Executes
     * after {@link #populateModel(org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode)}, so the given {@code model}
     * parameter will reflect any changes made in that method.
     * <p>
     * This default implementation does nothing.
     * </p>
     *
     * @param context             the operation context
     * @param operation           the operation being executed
     * @param model               persistent configuration model node that corresponds to the address of {@code operation}
     * @param verificationHandler step handler that can be added as a listener to any new services installed in order to
     *                            validate the services installed correctly during the
     *                            {@link org.jboss.as.controller.OperationContext.Stage#VERIFY VERIFY stage}
     * @param newControllers      holder for the {@link org.jboss.msc.service.ServiceController} for any new services installed by the method. The
     *                            method should add the {@code ServiceController} for any new services to this list. If the
     *                            overall operation needs to be rolled back, the list will be used in
     *                            {@link #rollbackRuntime(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, java.util.List)}  to automatically removed
     *                            the newly added services
     * @throws org.jboss.as.controller.OperationFailedException
     *          if {@code operation} is invalid or updating the runtime otherwise fails
     */
    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        if (context.isNormalServer() && !context.isBooting()) {
            context.reloadRequired();
        }

        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                if (context.isNormalServer() && !context.isBooting()) {
                    context.revertReloadRequired();
                }
            }
        });


    }
}
