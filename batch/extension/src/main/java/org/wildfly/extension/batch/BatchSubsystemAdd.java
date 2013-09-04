/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch;

import java.util.List;
import java.util.Properties;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.batch.deployment.BatchDependencyProcessor;
import org.wildfly.extension.batch.deployment.BatchEnvironmentProcessor;
import org.wildfly.extension.batch.services.BatchPropertiesService;
import org.wildfly.extension.batch.services.BatchServiceNames;

/**
 * Handler responsible for adding the subsystem resource to the model.
 */
class BatchSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final BatchSubsystemAdd INSTANCE = new BatchSubsystemAdd();

    private BatchSubsystemAdd() {
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        // TODO (jrp) is there a way to validate the JNDI name?
        BatchSubsystemDefinition.JOB_REPOSITORY.validateAndSet(operation, model);
    }

    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
                                ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(BatchSubsystemDefinition.NAME,
                        Phase.DEPENDENCIES, Phase.DEPENDENCIES_BATCH, new BatchDependencyProcessor());

            }
        }, OperationContext.Stage.RUNTIME);

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(BatchSubsystemDefinition.NAME,
                        Phase.POST_MODULE, Phase.POST_MODULE_BATCH_ENVIRONMENT, new BatchEnvironmentProcessor());

            }
        }, OperationContext.Stage.RUNTIME);

        final String jobRepositoryType = BatchSubsystemDefinition.JOB_REPOSITORY.resolveModelAttribute(context, model).asString();

        // Create the BatchEnvironment
        final BatchPropertiesService service = new BatchPropertiesService();
        if (BatchSubsystemDefinition.IN_MEMORY.equals(jobRepositoryType)) {
            service.addProperty("job-repository-type", jobRepositoryType);
        } else {
            service.addProperty("job-repository-type", "jdbc");
            service.addProperty("datasource-jndi", jobRepositoryType);
        }
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ServiceBuilder<Properties> builder = serviceTarget.addService(BatchServiceNames.BATCH_SERVICE_NAME, service);
        newControllers.add(builder.install());
    }
}
