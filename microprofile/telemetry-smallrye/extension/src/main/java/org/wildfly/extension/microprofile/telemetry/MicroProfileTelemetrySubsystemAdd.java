/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.telemetry;

import static org.wildfly.extension.microprofile.telemetry.MicroProfileTelemetryExtensionLogger.MPTEL_LOGGER;

import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

public class MicroProfileTelemetrySubsystemAdd extends AbstractBoottimeAddStepHandler {
    private final AtomicReference<WildFlyOpenTelemetryConfig> openTelemetryConfig = new AtomicReference<>();

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

        ServiceInstaller.builder(ServiceDependency.on(WildFlyOpenTelemetryConfig.SERVICE_DESCRIPTOR))
                .withCaptor(openTelemetryConfig::set)
                .build()
                .install(context);


        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(
                        MicroProfileTelemetryExtension.SUBSYSTEM_NAME,
                        Phase.DEPENDENCIES,
                        Phase.DEPENDENCIES_MICROPROFILE_TELEMETRY,
                        new MicroProfileTelemetryDependencyProcessor()
                );
                processorTarget.addDeploymentProcessor(
                        MicroProfileTelemetryExtension.SUBSYSTEM_NAME,
                        Phase.POST_MODULE,
                        Phase.POST_MODULE_MICROPROFILE_TELEMETRY,
                        new MicroProfileTelemetryDeploymentProcessor(openTelemetryConfig::get));
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
