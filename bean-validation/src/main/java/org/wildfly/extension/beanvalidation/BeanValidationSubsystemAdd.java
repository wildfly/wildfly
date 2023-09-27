/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.beanvalidation;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.ee.beanvalidation.BeanValidationDeploymentDependenciesProcessor;
import org.jboss.as.ee.beanvalidation.BeanValidationFactoryDeployer;
import org.jboss.as.ee.beanvalidation.BeanValidationResourceReferenceProcessorRegistryProcessor;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

import static org.wildfly.extension.beanvalidation.logging.BeanValidationLogger.ROOT_LOGGER;


/**
 * Handler that adds the Jakarta Bean Validation subsystem.
 *
 * @author Eduardo Martins
 */
class BeanValidationSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final BeanValidationSubsystemAdd INSTANCE = new BeanValidationSubsystemAdd();

    private BeanValidationSubsystemAdd() {
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                ROOT_LOGGER.debug("Activating Jakarta Bean Validation subsystem");
                processorTarget.addDeploymentProcessor(BeanValidationExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_BEAN_VALIDATION_RESOURCE_INJECTION_REGISTRY, new BeanValidationResourceReferenceProcessorRegistryProcessor());
                processorTarget.addDeploymentProcessor(BeanValidationExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_BEAN_VALIDATION, new BeanValidationDeploymentDependenciesProcessor());
                processorTarget.addDeploymentProcessor(BeanValidationExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_VALIDATOR_FACTORY, new BeanValidationFactoryDeployer());
            }
        }, OperationContext.Stage.RUNTIME);
    }

}
