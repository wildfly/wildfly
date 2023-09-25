/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import jakarta.batch.operations.JobOperator;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * A handler to assist with updating the dynamic batch job resources.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class JobOperationReadOnlyStepHandler extends JobOperationStepHandler {

    /**
     * Creates a step handler with a read-only {@link JobOperator}.
     */
    protected JobOperationReadOnlyStepHandler() {
        super(false);
    }

    @Override
    protected void execute(final OperationContext context, final ModelNode operation, final WildFlyJobOperator jobOperator) throws OperationFailedException {
        updateModel(context, context.getResult(), jobOperator, context.getCurrentAddressValue());
    }

    /**
     * Updates the model with information from the {@link jakarta.batch.operations.JobOperator JobOperator}.
     *
     * @param context     the operation context used
     * @param model       the model to update
     * @param jobOperator the job operator
     * @param jobName     the name of the job
     *
     * @throws OperationFailedException if an update failure occurs
     */
    protected abstract void updateModel(OperationContext context, ModelNode model, WildFlyJobOperator jobOperator, String jobName) throws OperationFailedException;
}
