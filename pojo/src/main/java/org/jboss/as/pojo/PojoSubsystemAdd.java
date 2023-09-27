/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

/**
 * Pojo subsystem add.
 * Define processors for POJO config handling.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Emanuel Muckenhuber
 */
class PojoSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final PojoSubsystemAdd INSTANCE = new PojoSubsystemAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) {

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(PojoExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_POJO_DEPLOYMENT, new KernelDeploymentParsingProcessor());
                processorTarget.addDeploymentProcessor(PojoExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.POST_MODULE_POJO, new KernelDeploymentModuleProcessor());
                processorTarget.addDeploymentProcessor(PojoExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_POJO_DEPLOYMENT, new ParsedKernelDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
