/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.DEFAULT_BATCH_DELAY;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.DEFAULT_EXPORT_TIMEOUT;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.DEFAULT_MAX_EXPORT_BATCH_SIZE;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.DEFAULT_MAX_QUEUE_SIZE;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.EXPORTER_OTLP;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_EXPORTER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_SAMPLER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_SPAN_PROCESSOR;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.OPENTELEMETRY_CAPABILITY_NAME;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.SPAN_PROCESSOR_BATCH;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
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

/*
 * For future reference: https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#jaeger-exporter
 */

class OpenTelemetrySubsystemDefinition extends PersistentResourceDefinition {
    static final String OPENTELEMETRY_MODULE = "org.wildfly.extension.opentelemetry";
    public static final String API_MODULE = "org.wildfly.extension.opentelemetry-api";
    public static final String[] EXPORTED_MODULES = {
            "io.opentelemetry.api",
            "io.opentelemetry.context",
            "io.opentelemetry.exporter",
            "io.opentelemetry.sdk",
            "io.smallrye.opentelemetry",
            "io.vertx.core",
            "io.vertx.grpc-client",
            "io.netty.netty-buffer"
    };

    static final RuntimeCapability<Void> OPENTELEMETRY_CAPABILITY =
            RuntimeCapability.Builder.of(OPENTELEMETRY_CAPABILITY_NAME)
                    .addRequirements(WELD_CAPABILITY_NAME)
                    .build();

    public static final WildFlyOpenTelemetryConfigSupplier CONFIG_SUPPLIER = new WildFlyOpenTelemetryConfigSupplier();
    static final RuntimeCapability<WildFlyOpenTelemetryConfigSupplier> OPENTELEMETRY_CONFIG_CAPABILITY =
            RuntimeCapability.Builder.of(OPENTELEMETRY_MODULE + ".config", false, CONFIG_SUPPLIER).build();
    @Deprecated
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
            .setDefaultValue(new ModelNode(EXPORTER_OTLP))
            .setValidator(new StringAllowedValuesValidator(OpenTelemetryConfigurationConstants.ALLOWED_EXPORTERS))
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition ENDPOINT = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.ENDPOINT, ModelType.STRING, true)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_EXPORTER)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(OpenTelemetryConfigurationConstants.DEFAULT_OTLP_ENDPOINT))
            .build();

    @Deprecated
    public static final SimpleAttributeDefinition SPAN_PROCESSOR_TYPE = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.SPAN_PROCESSOR_TYPE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setXmlName(OpenTelemetryConfigurationConstants.TYPE)
            .setAttributeGroup(GROUP_SPAN_PROCESSOR)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(SPAN_PROCESSOR_BATCH))
            .setValidator(new StringAllowedValuesValidator(OpenTelemetryConfigurationConstants.ALLOWED_SPAN_PROCESSORS))
            .build();

    public static final SimpleAttributeDefinition BATCH_DELAY = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.BATCH_DELAY, ModelType.LONG, true)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_SPAN_PROCESSOR)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(DEFAULT_BATCH_DELAY))
            .build();

    public static final SimpleAttributeDefinition MAX_QUEUE_SIZE = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.MAX_QUEUE_SIZE, ModelType.LONG, true)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_SPAN_PROCESSOR)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(DEFAULT_MAX_QUEUE_SIZE))
            .build();

    public static final SimpleAttributeDefinition MAX_EXPORT_BATCH_SIZE = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.MAX_EXPORT_BATCH_SIZE, ModelType.LONG, true)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_SPAN_PROCESSOR)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(DEFAULT_MAX_EXPORT_BATCH_SIZE))
            .build();

    public static final SimpleAttributeDefinition EXPORT_TIMEOUT = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.EXPORT_TIMEOUT, ModelType.LONG, true)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_SPAN_PROCESSOR)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(DEFAULT_EXPORT_TIMEOUT))
            .build();

    public static final SimpleAttributeDefinition SAMPLER = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.SAMPLER_TYPE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setXmlName(OpenTelemetryConfigurationConstants.TYPE)
            .setAttributeGroup(GROUP_SAMPLER)
            .setValidator(new StringAllowedValuesValidator(OpenTelemetryConfigurationConstants.ALLOWED_SAMPLERS))
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RATIO = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.SAMPLER_RATIO, ModelType.DOUBLE, true)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_SAMPLER)
            .setValidator((parameterName, value) -> {
                if (value.isDefined() && value.getType() != ModelType.EXPRESSION) {
                    double val = value.asDouble();
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
                OpenTelemetrySubsystemExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(new OpenTelemetrySubsystemAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(OPENTELEMETRY_CAPABILITY, OPENTELEMETRY_CONFIG_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    static class WildFlyOpenTelemetryConfigSupplier implements Supplier<OpenTelemetryConfig>, Consumer<OpenTelemetryConfig> {
        private OpenTelemetryConfig config;
        @Override
        public void accept(OpenTelemetryConfig openTelemetryConfig) {
            this.config = openTelemetryConfig;
        }

        @Override
        public OpenTelemetryConfig get() {
            return config;
        }
    }
}
