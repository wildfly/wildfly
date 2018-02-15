/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch.jberet.deployment;

import javax.batch.operations.JobOperator;

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
     * Updates the model with information from the {@link javax.batch.operations.JobOperator JobOperator}.
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
