/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.eesecurity;


import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;

/**
 * The EE subsystem add update handler.
 *
 * @author Stuart Douglas
 */
class EESecuritySubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final EESecuritySubsystemAdd INSTANCE = new EESecuritySubsystemAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    protected void performBoottime(final OperationContext context, ModelNode operation, Resource resource) {

        final ServiceTarget serviceTarget = context.getServiceTarget();
        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(EESecurityExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EE_SECURITY_ANNOTATIONS, new EESecurityAnnotationProcessor());
                processorTarget.addDeploymentProcessor(EESecurityExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_EE_SECURITY, new EESecurityDependencyProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
