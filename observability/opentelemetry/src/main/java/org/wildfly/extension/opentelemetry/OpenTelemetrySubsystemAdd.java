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

import static org.wildfly.extension.opentelemetry.OpenTelemetrySubsystemDefinition.CONFIG_SUPPLIER;

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

    OpenTelemetrySubsystemAdd() {
        super(OpenTelemetrySubsystemDefinition.ATTRIBUTES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performBoottime(context, operation, model);

        final WildFlyOpenTelemetryConfig config = new WildFlyOpenTelemetryConfig(
                OpenTelemetrySubsystemDefinition.SERVICE_NAME.resolveModelAttribute(context, model).asStringOrNull(),
                OpenTelemetrySubsystemDefinition.EXPORTER.resolveModelAttribute(context, model).asString(),
                OpenTelemetrySubsystemDefinition.ENDPOINT.resolveModelAttribute(context, model).asStringOrNull(),
                OpenTelemetrySubsystemDefinition.BATCH_DELAY.resolveModelAttribute(context, model).asLongOrNull(),
                OpenTelemetrySubsystemDefinition.MAX_QUEUE_SIZE.resolveModelAttribute(context, model).asLongOrNull(),
                OpenTelemetrySubsystemDefinition.MAX_EXPORT_BATCH_SIZE.resolveModelAttribute(context, model).asLongOrNull(),
                OpenTelemetrySubsystemDefinition.EXPORT_TIMEOUT.resolveModelAttribute(context, model).asLongOrNull(),
                OpenTelemetrySubsystemDefinition.SAMPLER.resolveModelAttribute(context, model).asStringOrNull(),
                OpenTelemetrySubsystemDefinition.RATIO.resolveModelAttribute(context, model).asDoubleOrNull()
        );

        CONFIG_SUPPLIER.accept(config);

        boolean mpTelemetryInstalled = context.getCapabilityServiceSupport().hasCapability("org.wildfly.extension.microprofile.telemetry");

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
                        new OpenTelemetryDeploymentProcessor(!mpTelemetryInstalled, config));
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
