/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.ejb3.deployment.processors.merging.AsynchronousMergingProcessor;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * A {@link org.jboss.as.controller.AbstractBoottimeAddStepHandler} to handle the add operation for the Jakarta Enterprise Beans
 * remote service, in the Jakarta Enterprise Beans subsystem
 * <p/>
 *
 * @author Stuart Douglas
 */
public class EJB3AsyncServiceAdd extends AbstractBoottimeAddStepHandler {

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {

        final String threadPoolName = EJB3AsyncResourceDefinition.THREAD_POOL_NAME.resolveModelAttribute(context, model).asString();

        final ServiceName threadPoolServiceName = context.getCapabilityServiceName(EJB3SubsystemRootResourceDefinition.EXECUTOR_SERVICE_DESCRIPTOR, threadPoolName);

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                ROOT_LOGGER.debug("Adding Jakarta Enterprise Beans @Asynchronous support");
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_ASYNCHRONOUS_MERGE, new AsynchronousMergingProcessor(threadPoolServiceName));
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
