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
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.EXPORTER_JAEGER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.EXPORTER_OTLP;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_EXPORTER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_SAMPLER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_SPAN_PROCESSOR;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.OPENTELEMETRY_CAPABILITY_NAME;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.SPAN_PROCESSOR_BATCH;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.SUBSYSTEM_PATH;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.SUBSYSTEM_RESOLVER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.VERTX_DISABLE_DNS_RESOLVER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryExtensionLogger.OTEL_LOGGER;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringAllowedValuesValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/*
 * For future reference: https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#jaeger-exporter
 */

class OpenTelemetrySubsystemRegistrar implements SubsystemResourceDefinitionRegistrar, ResourceServiceConfigurator {
    public static final boolean IS_WILDFLY_PREVIEW;

    static {
        // Temporary check to determine if the server is WildFly Standard or WildFly Preview. This is
        // a *temporary* check, so there should be no long-term dependencies on this. -- jdl
        boolean classFound = false;
        try {
            Class.forName("io.smallrye.opentelemetry.implementation.exporters.metrics.VertxMetricsExporterProvider");
            classFound = true;
        } catch (ClassNotFoundException e) {
        }
        IS_WILDFLY_PREVIEW = classFound;
    }

    public static final String API_MODULE = "org.wildfly.extension.opentelemetry-api";
    public static final List<String> EXPORTED_MODULES = new ArrayList<>();

    static final RuntimeCapability<Void> OPENTELEMETRY_CAPABILITY =
            RuntimeCapability.Builder.of(OPENTELEMETRY_CAPABILITY_NAME)
                    .addRequirements(WELD_CAPABILITY_NAME)
                    .build();

    public static final RuntimeCapability<Void> OPENTELEMETRY_CONFIG_CAPABILITY =
            RuntimeCapability.Builder.of(WildFlyOpenTelemetryConfig.SERVICE_DESCRIPTOR).build();
    ;

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

    public static final List<AttributeDefinition> ATTRIBUTES = List.of(
            SERVICE_NAME, EXPORTER, ENDPOINT, SPAN_PROCESSOR_TYPE, BATCH_DELAY, MAX_QUEUE_SIZE, MAX_EXPORT_BATCH_SIZE,
            EXPORT_TIMEOUT, SAMPLER, RATIO
    );

    private final AtomicReference<WildFlyOpenTelemetryConfig> openTelemetryConfig = new AtomicReference<>();

    static {
        // We need to disable vertx's DNS resolver as it causes failures under k8s
        if (System.getProperty(VERTX_DISABLE_DNS_RESOLVER) == null) {
            System.setProperty(VERTX_DISABLE_DNS_RESOLVER, "true");
        }

        EXPORTED_MODULES.addAll(List.of(
                "io.opentelemetry.api",
                "io.opentelemetry.api.events",
                "io.opentelemetry.context",
                "io.opentelemetry.exporter",
                "io.opentelemetry.otlp",
                "io.opentelemetry.sdk",
                "io.opentelemetry.semconv",
                "io.smallrye.opentelemetry"));
        if (IS_WILDFLY_PREVIEW) {
            EXPORTED_MODULES.add("io.opentelemetry.instrumentation.api");
            EXPORTED_MODULES.add("io.opentelemetry.semconv");
        }
    }

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent,
                                                   ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration =
                parent.registerSubsystemModel(ResourceDefinition.builder(ResourceRegistration.of(SUBSYSTEM_PATH),
                        SUBSYSTEM_RESOLVER).build());
        ResourceDescriptor descriptor = ResourceDescriptor.builder(SUBSYSTEM_RESOLVER)
                .addCapability(OPENTELEMETRY_CAPABILITY)
                .addCapability(OPENTELEMETRY_CONFIG_CAPABILITY)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .addAttributes(ATTRIBUTES)
                .withAddOperationRestartFlag(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .withRemoveOperationRestartFlag(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .withDeploymentChainContributor(target -> {
                    target.addDeploymentProcessor(OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME,
                            Phase.DEPENDENCIES,
                            0x18C5, // TODO: Update once WF Core is updated
                            new OpenTelemetryDependencyProcessor());
                    target.addDeploymentProcessor(OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME,
                            Phase.POST_MODULE,
                            0x3810, // TODO: Update once WF Core is updated
                            new OpenTelemetryDeploymentProcessor(this.openTelemetryConfig::get));
                })
                .build();

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String exporter = OpenTelemetrySubsystemRegistrar.EXPORTER.resolveModelAttribute(context, model).asString();
        validateExporter(context, exporter);

        final WildFlyOpenTelemetryConfig config = new WildFlyOpenTelemetryConfig(
                OpenTelemetrySubsystemRegistrar.SERVICE_NAME.resolveModelAttribute(context, model).asStringOrNull(),
                exporter,
                OpenTelemetrySubsystemRegistrar.ENDPOINT.resolveModelAttribute(context, model).asStringOrNull(),
                OpenTelemetrySubsystemRegistrar.BATCH_DELAY.resolveModelAttribute(context, model).asLongOrNull(),
                OpenTelemetrySubsystemRegistrar.MAX_QUEUE_SIZE.resolveModelAttribute(context, model).asLongOrNull(),
                OpenTelemetrySubsystemRegistrar.MAX_EXPORT_BATCH_SIZE.resolveModelAttribute(context, model).asLongOrNull(),
                OpenTelemetrySubsystemRegistrar.EXPORT_TIMEOUT.resolveModelAttribute(context, model).asLongOrNull(),
                OpenTelemetrySubsystemRegistrar.SPAN_PROCESSOR_TYPE.resolveModelAttribute(context, model).asStringOrNull(),
                OpenTelemetrySubsystemRegistrar.SAMPLER.resolveModelAttribute(context, model).asStringOrNull(),
                OpenTelemetrySubsystemRegistrar.RATIO.resolveModelAttribute(context, model).asDoubleOrNull(),
                context.getCapabilityServiceSupport().hasCapability("org.wildfly.extension.microprofile.telemetry")
        );

        return CapabilityServiceInstaller.builder(OPENTELEMETRY_CONFIG_CAPABILITY, config)
                .withCaptor(openTelemetryConfig::set)
                .build();
    }

    private void validateExporter(OperationContext context, String exporter) throws OperationFailedException {
        if (EXPORTER_JAEGER.equals(exporter)) {
            if (context.isNormalServer()) {
                context.setRollbackOnly();
                throw new OperationFailedException(OTEL_LOGGER.jaegerIsNoLongerSupported());
            } else {
                OTEL_LOGGER.warn(OTEL_LOGGER.jaegerIsNoLongerSupported());
            }
        }
    }
}
