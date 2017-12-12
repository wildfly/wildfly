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

import java.util.Properties;
import javax.batch.operations.JobOperator;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.batch.jberet.BatchServiceNames;
import org.wildfly.extension.batch.jberet._private.BatchLogger;

/**
 * A handler to assist with batch operations that require a {@linkplain JobOperator}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class JobOperationStepHandler implements OperationStepHandler {
    private final boolean modify;

    /**
     * Creates a new step handler with a modifiable {@link JobOperator}.
     */
    JobOperationStepHandler() {
        this(true);
    }

    /**
     * Creates a new step handler.
     *
     * @param modify {@code true} if the {@link #execute(OperationContext, ModelNode, WildFlyJobOperator)} will modify a job
     *               repository, {@code false} for a read-only service
     */
    JobOperationStepHandler(final boolean modify) {
        this.modify = modify;
    }

    @Override
    public final void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        context.addStep(this::executeRuntime, OperationContext.Stage.RUNTIME);
    }

    private void executeRuntime(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final ServiceController<?> controller = context.getServiceRegistry(modify).getService(getServiceName(context));
        final WildFlyJobOperator jobOperator = (WildFlyJobOperator) controller.getService();
        execute(context, operation, jobOperator);
    }

    /**
     * Executes the step. Includes the {@linkplain JobOperator} for convenience.
     *
     * @param context     the operation context used
     * @param operation   the operation for the step
     * @param jobOperator the job operator
     *
     * @throws OperationFailedException if there is a step failure
     */
    protected abstract void execute(OperationContext context, ModelNode operation, WildFlyJobOperator jobOperator) throws OperationFailedException;

    static ModelNode resolveValue(final OperationContext context, final ModelNode operation, final AttributeDefinition attribute) throws OperationFailedException {
        final ModelNode value = new ModelNode();
        if (operation.has(attribute.getName())) {
            value.set(operation.get(attribute.getName()));
        }
        return attribute.resolveValue(context, value);
    }

    static Properties resolvePropertyValue(final OperationContext context, final ModelNode operation, final AttributeDefinition attribute) throws OperationFailedException {
        // Get the properties
        final Properties properties = new Properties();
        if (operation.hasDefined(attribute.getName())) {
            for (Property p : resolveValue(context, operation, attribute).asPropertyList()) {
                properties.put(p.getName(), p.getValue().asString());
            }
        }
        return properties;
    }

    static OperationFailedException createOperationFailure(final Throwable cause) {
        final String msg = cause.getLocalizedMessage();
        // OperationFailedException's don't log the cause, for debug purposes logging the failure could be useful
        BatchLogger.LOGGER.debugf(cause, "Failed to process batch operation: %s", msg);
        return new OperationFailedException(msg, cause);
    }


    private static ServiceName getServiceName(final OperationContext context) {
        final PathAddress address = context.getCurrentAddress();
        String deploymentName = null;
        String subdeploymentName = null;
        for (PathElement element : address) {
            if (ModelDescriptionConstants.DEPLOYMENT.equals(element.getKey())) {
                deploymentName = getRuntimeName(context, element);
            } else if (ModelDescriptionConstants.SUBDEPLOYMENT.endsWith(element.getKey())) {
                subdeploymentName = element.getValue();
            }
        }
        if (deploymentName == null) {
            throw BatchLogger.LOGGER.couldNotFindDeploymentName(address.toString());
        }
        if (subdeploymentName == null) {
            return BatchServiceNames.jobOperatorServiceName(deploymentName);
        }
        return BatchServiceNames.jobOperatorServiceName(deploymentName, subdeploymentName);
    }

    private static String getRuntimeName(final OperationContext context, final PathElement element) {
        final ModelNode deploymentModel = context.readResourceFromRoot(PathAddress.pathAddress(element), false).getModel();
        if (!deploymentModel.hasDefined(ModelDescriptionConstants.RUNTIME_NAME)) {
            throw BatchLogger.LOGGER.couldNotFindDeploymentName(context.getCurrentAddress().toString());
        }
        return deploymentModel.get(ModelDescriptionConstants.RUNTIME_NAME).asString();
    }
}
