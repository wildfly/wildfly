/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.opentelemetry;

import static org.wildfly.extension.opentelemetry.OpenTelemetryConfig.OpenTelemetryConfigBuilder;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.opentelemetry.deployment.OpenTelemetryDependencyProcessor;
import org.wildfly.extension.opentelemetry.deployment.OpenTelemetrySubsystemDeploymentProcessor;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="jasondlee@redhat.com">Jason Lee</a>
 */
public class OpenTelemetrySubsystemAdd extends AbstractBoottimeAddStepHandler {

    private OpenTelemetrySubsystemAdd() {
        super(OpenTelemetrySubsystemDefinition.ATTRIBUTES);
    }

    public static final OpenTelemetrySubsystemAdd INSTANCE = new OpenTelemetrySubsystemAdd();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performBoottime(context, operation, model);

        final OpenTelemetryConfig config = OpenTelemetryConfigBuilder.config()
                .withServiceName(OpenTelemetrySubsystemDefinition.SERVICE_NAME.resolveModelAttribute(context, model).asStringOrNull())
                .withExporter(OpenTelemetrySubsystemDefinition.EXPORTER.resolveModelAttribute(context, model).asString())
                .withEndpoint(OpenTelemetrySubsystemDefinition.ENDPOINT.resolveModelAttribute(context, model).asStringOrNull())
                .withSpanProcessor(OpenTelemetrySubsystemDefinition.SPAN_PROCESSOR_TYPE.resolveModelAttribute(context, model).asString())
                .withBatchDelay(OpenTelemetrySubsystemDefinition.BATCH_DELAY.resolveModelAttribute(context, model).asLong())
                .withMaxQueueSize(OpenTelemetrySubsystemDefinition.MAX_QUEUE_SIZE.resolveModelAttribute(context, model).asInt())
                .withMaxExportBatchSize(OpenTelemetrySubsystemDefinition.MAX_EXPORT_BATCH_SIZE.resolveModelAttribute(context, model).asInt())
                .withExportTimeout(OpenTelemetrySubsystemDefinition.EXPORT_TIMEOUT.resolveModelAttribute(context, model).asLong())
                .withSampler(OpenTelemetrySubsystemDefinition.SAMPLER.resolveModelAttribute(context, model).asStringOrNull())
                .withRatio(OpenTelemetrySubsystemDefinition.RATIO.resolveModelAttribute(context, model).asDoubleOrNull())
                .build();
        final OpenTelemetryHolder holder = new OpenTelemetryHolder(config);

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
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
