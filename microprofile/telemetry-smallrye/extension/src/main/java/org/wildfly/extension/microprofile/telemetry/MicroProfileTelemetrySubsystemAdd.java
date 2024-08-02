/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.telemetry;

import static org.wildfly.extension.microprofile.telemetry.MicroProfileTelemetryExtensionLogger.MPTEL_LOGGER;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;
import org.wildfly.service.ServiceDependency;

public class MicroProfileTelemetrySubsystemAdd extends AbstractBoottimeAddStepHandler {

    MicroProfileTelemetrySubsystemAdd() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        MPTEL_LOGGER.activatingSubsystem();

        super.performBoottime(context, operation, model);

        final ServiceDependency<WildFlyOpenTelemetryConfig> configServiceDependency =
                ServiceDependency.on(ServiceName.parse(WildFlyOpenTelemetryConfig.SERVICE_DESCRIPTOR.getName()));

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(
                        MicroProfileTelemetryExtension.SUBSYSTEM_NAME,
                        Phase.DEPENDENCIES,
                        Phase.DEPENDENCIES_MICROPROFILE_TELEMETRY,
                        new MicroProfileTelemetryDependencyProcessor(configServiceDependency)
                );
                processorTarget.addDeploymentProcessor(
                        MicroProfileTelemetryExtension.SUBSYSTEM_NAME,
                        Phase.POST_MODULE,
                        Phase.POST_MODULE_MICROPROFILE_TELEMETRY,
                        new MicroProfileTelemetryDeploymentProcessor(configServiceDependency));
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
