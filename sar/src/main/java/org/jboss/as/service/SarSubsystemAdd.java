/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import javax.management.MBeanServer;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.service.component.ServiceComponentProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 */
public class SarSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final SarSubsystemAdd INSTANCE = new SarSubsystemAdd();

    private SarSubsystemAdd() {
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    protected void performBoottime(final OperationContext context, ModelNode operation, ModelNode model) {

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {

                ServiceName jmxCapability = context.getCapabilityServiceName(SarExtension.JMX_CAPABILITY, MBeanServer.class);

                processorTarget.addDeploymentProcessor(SarExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_SAR_SUB_DEPLOY_CHECK, new SarSubDeploymentProcessor());
                processorTarget.addDeploymentProcessor(SarExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_SAR, new SarStructureProcessor());
                processorTarget.addDeploymentProcessor(SarExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_SAR_MODULE, new SarModuleDependencyProcessor());
                processorTarget.addDeploymentProcessor(SarExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_SERVICE_DEPLOYMENT, new ServiceDeploymentParsingProcessor());
                processorTarget.addDeploymentProcessor(SarExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_SAR_SERVICE_COMPONENT, new ServiceComponentProcessor());
                processorTarget.addDeploymentProcessor(SarExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_SERVICE_DEPLOYMENT, new ParsedServiceDeploymentProcessor(jmxCapability));
            }
        }, OperationContext.Stage.RUNTIME);

    }
}
