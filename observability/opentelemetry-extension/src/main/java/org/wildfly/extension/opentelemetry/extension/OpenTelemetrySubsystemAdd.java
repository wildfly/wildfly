package org.wildfly.extension.opentelemetry.extension;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.opentelemetry.deployment.OpenTelemetryDependencyProcessor;
import org.wildfly.extension.opentelemetry.deployment.OpenTelemetrySubsystemDeploymentProcessor;
import org.wildfly.extension.opentelemetry.extension.model.OpenTelemetryConfig;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="jasondlee@redhat.com">Jason Lee</a>
 */
public class OpenTelemetrySubsystemAdd extends AbstractBoottimeAddStepHandler {

    OpenTelemetrySubsystemAdd() {
        super(OpenTelemetrySubsystemDefinition.ATTRIBUTES);
    }

    public static final OpenTelemetrySubsystemAdd INSTANCE = new OpenTelemetrySubsystemAdd();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performBoottime(context, operation, model);

        final OpenTelemetryConfig config = OpenTelemetryConfig.OpenTelemetryConfigBuilder.config()
                .withServiceName(OpenTelemetrySubsystemDefinition.SERVICE_NAME.resolveModelAttribute(context, model).asStringOrNull())
                .withExporter(OpenTelemetrySubsystemDefinition.EXPORTER.resolveModelAttribute(context, model).asString())
                .withEndpoint(OpenTelemetrySubsystemDefinition.ENDPOINT.resolveModelAttribute(context, model).asStringOrNull())
                .withSpanProcessor(OpenTelemetrySubsystemDefinition.SPAN_PROCESSOR.resolveModelAttribute(context, model).asString())
                .withBatchDelay(OpenTelemetrySubsystemDefinition.BATCH_DELAY.resolveModelAttribute(context, model).asLong())
                .withMaxQueueSize(OpenTelemetrySubsystemDefinition.MAX_QUEUE_SIZE.resolveModelAttribute(context, model).asInt())
                .withMaxExportBatchSize(OpenTelemetrySubsystemDefinition.MAX_EXPORT_BATCH_SIZE.resolveModelAttribute(context, model).asInt())
                .withExportTimeout(OpenTelemetrySubsystemDefinition.EXPORT_TIMEOUT.resolveModelAttribute(context, model).asLong())
                .withSampler(OpenTelemetrySubsystemDefinition.SAMPLER.resolveModelAttribute(context, model).asStringOrNull())
                .withSamplerArg(OpenTelemetrySubsystemDefinition.SAMPLER_ARG.resolveModelAttribute(context, model).asDouble(1.0d))
                .build();
        final OpenTelemetryHolder holder = new OpenTelemetryHolder(config);

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
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
                        new OpenTelemetrySubsystemDeploymentProcessor(holder));
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
