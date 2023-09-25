/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.faulttolerance;

import static org.wildfly.extension.microprofile.faulttolerance.MicroProfileFaultToleranceLogger.ROOT_LOGGER;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.microprofile.faulttolerance.deployment.MicroProfileFaultToleranceDependenciesProcessor;
import org.wildfly.extension.microprofile.faulttolerance.deployment.MicroProfileFaultToleranceDeploymentProcessor;

/**
 * @author Radoslav Husar
 */
public class MicroProfileFaultToleranceServiceHandler implements ResourceServiceHandler, Consumer<DeploymentProcessorTarget> {

    @Override
    public void installServices(OperationContext context, ModelNode model) {
        ROOT_LOGGER.activatingSubsystem();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) {
    }

    @Override
    public void accept(DeploymentProcessorTarget deploymentProcessorTarget) {
        deploymentProcessorTarget.addDeploymentProcessor(MicroProfileFaultToleranceExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_MICROPROFILE_FAULT_TOLERANCE, new MicroProfileFaultToleranceDependenciesProcessor());
        deploymentProcessorTarget.addDeploymentProcessor(MicroProfileFaultToleranceExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_MICROPROFILE_FAULT_TOLERANCE, new MicroProfileFaultToleranceDeploymentProcessor());
    }

}
