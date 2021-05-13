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

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_EXPORTER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_SAMPLER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_SPAN_PROCESSOR;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.OPENTELEMETRY_CAPABILITY_NAME;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringAllowedValuesValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.opentelemetry.deployment.OpenTelemetryExtensionLogger;

/*
 * For future reference: https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#jaeger-exporter
 */

public class OpenTelemetrySubsystemDefinition extends PersistentResourceDefinition {
    public static final String DEFAULT_ENDPOINT = "http://localhost:14250";
    private static final String[] ALLOWED_EXPORTERS = {"jaeger", "otlp"};
    private static final String[] ALLOWED_SAMPLERS = {"on", "off", "ratio"};
    private static final String[] ALLOWED_SPAN_PROCESSORS = {"batch", "simple"};

    public static final String[] EXPORTED_MODULES = {
            "io.opentelemetry.api",
            "io.opentelemetry.context",
            "org.wildfly.extension.opentelemetry-api"
    };

    static final RuntimeCapability<Void> OPENTELEMETRY_CAPABILITY =
            RuntimeCapability.Builder.of(OPENTELEMETRY_CAPABILITY_NAME)
                    .addRequirements(WELD_CAPABILITY_NAME)
                    .build();

    public static final SimpleAttributeDefinition SERVICE_NAME = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.SERVICE_NAME, ModelType.STRING, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition EXPORTER = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.EXPORTER_TYPE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_EXPORTER)
            .setXmlName(OpenTelemetryConfigurationConstants.TYPE)
            .setDefaultValue(new ModelNode("jaeger"))
            .setAllowedValues(ALLOWED_EXPORTERS)
            .setValidator(new StringAllowedValuesValidator(ALLOWED_EXPORTERS))
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition ENDPOINT = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.ENDPOINT, ModelType.STRING, true)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_EXPORTER)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(DEFAULT_ENDPOINT))
            .build();


    public static final SimpleAttributeDefinition SPAN_PROCESSOR_TYPE = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.SPAN_PROCESSOR_TYPE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setXmlName(OpenTelemetryConfigurationConstants.TYPE)
            .setAttributeGroup(GROUP_SPAN_PROCESSOR)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode("batch"))
            .setAllowedValues(ALLOWED_SPAN_PROCESSORS)
            .setValidator(new StringAllowedValuesValidator(ALLOWED_SPAN_PROCESSORS))
            .build();

    public static final SimpleAttributeDefinition BATCH_DELAY = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.BATCH_DELAY, ModelType.LONG, true)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_SPAN_PROCESSOR)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(5000))
            .build();

    public static final SimpleAttributeDefinition MAX_QUEUE_SIZE = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.MAX_QUEUE_SIZE, ModelType.LONG, true)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_SPAN_PROCESSOR)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(2048))
            .build();

    public static final SimpleAttributeDefinition MAX_EXPORT_BATCH_SIZE = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.MAX_EXPORT_BATCH_SIZE, ModelType.LONG, true)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_SPAN_PROCESSOR)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(512))
            .build();

    public static final SimpleAttributeDefinition EXPORT_TIMEOUT = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.EXPORT_TIMEOUT, ModelType.LONG, true)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_SPAN_PROCESSOR)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(30000))
            .build();

    public static final SimpleAttributeDefinition SAMPLER = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.SAMPLER_TYPE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setXmlName(OpenTelemetryConfigurationConstants.TYPE)
            .setAttributeGroup(GROUP_SAMPLER)
            .setAllowedValues(ALLOWED_SAMPLERS)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RATIO = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.ratio, ModelType.DOUBLE, true)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_SAMPLER)
            .setValidator((parameterName, value) -> {
                if (value.isDefined() && value.getType() != ModelType.EXPRESSION) {
                    long val = value.asLong();
                    if (val < 0.0 || val > 1.0) {
                        throw new OperationFailedException(OpenTelemetryExtensionLogger.OTEL_LOGGER.invalidRatio());
                    }
                }
            })
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {
            SERVICE_NAME, EXPORTER, ENDPOINT, SPAN_PROCESSOR_TYPE, BATCH_DELAY, MAX_QUEUE_SIZE, MAX_EXPORT_BATCH_SIZE,
            EXPORT_TIMEOUT, SAMPLER, RATIO
    };

    protected OpenTelemetrySubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(OpenTelemetrySubsystemExtension.SUBSYSTEM_PATH,
                OpenTelemetrySubsystemExtension.getResourceDescriptionResolver())
                .setAddHandler(OpenTelemetrySubsystemAdd.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(OPENTELEMETRY_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }
}
