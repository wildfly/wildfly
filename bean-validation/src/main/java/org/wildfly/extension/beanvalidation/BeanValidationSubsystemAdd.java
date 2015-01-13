/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
 * Handler that adds the bean validation subsystem.
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
                ROOT_LOGGER.debug("Activating Bean Validation subsystem");
                processorTarget.addDeploymentProcessor(BeanValidationExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_BEAN_VALIDATION_RESOURCE_INJECTION_REGISTRY, new BeanValidationResourceReferenceProcessorRegistryProcessor());
                processorTarget.addDeploymentProcessor(BeanValidationExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_BEAN_VALIDATION, new BeanValidationDeploymentDependenciesProcessor());
                processorTarget.addDeploymentProcessor(BeanValidationExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_VALIDATOR_FACTORY, new BeanValidationFactoryDeployer());
            }
        }, OperationContext.Stage.RUNTIME);
    }

}
