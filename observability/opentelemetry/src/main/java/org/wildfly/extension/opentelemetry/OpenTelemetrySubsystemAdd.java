/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.wildfly.extension.opentelemetry.OpenTelemetrySubsystemDefinition.CONFIG_SUPPLIER;
import static org.wildfly.extension.opentelemetry.OpenTelemetrySubsystemDefinition.validateExporter;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="jasondlee@redhat.com">Jason Lee</a>
 */
class OpenTelemetrySubsystemAdd extends AbstractBoottimeAddStepHandler {

    private static final String VERTX_DISABLE_DNS_RESOLVER = "vertx.disableDnsResolver";

    OpenTelemetrySubsystemAdd() {
        super(OpenTelemetrySubsystemDefinition.ATTRIBUTES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performBoottime(context, operation, model);

        String exporter = OpenTelemetrySubsystemDefinition.EXPORTER.resolveModelAttribute(context, model).asString();
        validateExporter(context, exporter);

        final WildFlyOpenTelemetryConfig config = new WildFlyOpenTelemetryConfig(
                OpenTelemetrySubsystemDefinition.SERVICE_NAME.resolveModelAttribute(context, model).asStringOrNull(),
                exporter,
                OpenTelemetrySubsystemDefinition.ENDPOINT.resolveModelAttribute(context, model).asStringOrNull(),
                OpenTelemetrySubsystemDefinition.BATCH_DELAY.resolveModelAttribute(context, model).asLongOrNull(),
                OpenTelemetrySubsystemDefinition.MAX_QUEUE_SIZE.resolveModelAttribute(context, model).asLongOrNull(),
                OpenTelemetrySubsystemDefinition.MAX_EXPORT_BATCH_SIZE.resolveModelAttribute(context, model).asLongOrNull(),
                OpenTelemetrySubsystemDefinition.EXPORT_TIMEOUT.resolveModelAttribute(context, model).asLongOrNull(),
                OpenTelemetrySubsystemDefinition.SPAN_PROCESSOR_TYPE.resolveModelAttribute(context, model).asStringOrNull(),
                OpenTelemetrySubsystemDefinition.SAMPLER.resolveModelAttribute(context, model).asStringOrNull(),
                OpenTelemetrySubsystemDefinition.RATIO.resolveModelAttribute(context, model).asDoubleOrNull()
        );

        CONFIG_SUPPLIER.accept(config);

        boolean mpTelemetryInstalled = context.getCapabilityServiceSupport()
                .hasCapability("org.wildfly.extension.microprofile.telemetry");

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                // We need to disable vertx's DNS resolver as it causes failures under k8s
                if (System.getProperty(VERTX_DISABLE_DNS_RESOLVER) == null) {
                    System.setProperty(VERTX_DISABLE_DNS_RESOLVER, "true");
                }
                processorTarget.addDeploymentProcessor(
                        OpenTelemetrySubsystemExtension.SUBSYSTEM_NAME,
                        Phase.DEPENDENCIES,
                        0x1910,
                        new OpenTelemetryDependencyProcessor()
                );
                processorTarget.addDeploymentProcessor(
                        OpenTelemetrySubsystemExtension.SUBSYSTEM_NAME,
                        Phase.POST_MODULE,
                        0x3810,
                        new OpenTelemetryDeploymentProcessor(!mpTelemetryInstalled, config));
            }
        }, OperationContext.Stage.RUNTIME);

    }
}
