/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES_OPENTELEMETRY;
import static org.jboss.as.server.deployment.Phase.POST_MODULE;
import static org.jboss.as.server.deployment.Phase.POST_MODULE_OPENTELEMETRY;
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
    private static final String CAPABILITY_NAME_METRICS = "org.wildfly.extension.metrics.scan";
    private static final String CAPABILITY_NAME_MICROMETER = "org.wildfly.extension.micrometer";

    static final RuntimeCapability<Void> OPENTELEMETRY_CAPABILITY =
            RuntimeCapability.Builder.of(OPENTELEMETRY_CAPABILITY_NAME)
                    .addRequirements(WELD_CAPABILITY_NAME)
                    .build();

    public static final RuntimeCapability<Void> OPENTELEMETRY_CONFIG_CAPABILITY =
            RuntimeCapability.Builder.of(WildFlyOpenTelemetryConfig.SERVICE_DESCRIPTOR).build();

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
    }

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent,
                                                   ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration =
                parent.registerSubsystemModel(ResourceDefinition.builder(ResourceRegistration.of(SUBSYSTEM_PATH), SUBSYSTEM_RESOLVER).build());
        ResourceDescriptor descriptor = ResourceDescriptor.builder(SUBSYSTEM_RESOLVER)
                .addCapability(OPENTELEMETRY_CAPABILITY)
                .addCapability(OPENTELEMETRY_CONFIG_CAPABILITY)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .addAttributes(ATTRIBUTES)
                .withAddOperationRestartFlag(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .withRemoveOperationRestartFlag(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .withDeploymentChainContributor(target -> {
                    target.addDeploymentProcessor(OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME,
                            DEPENDENCIES,
                            DEPENDENCIES_OPENTELEMETRY,
                            new OpenTelemetryDependencyProcessor());
                    target.addDeploymentProcessor(OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME,
                            POST_MODULE,
                            POST_MODULE_OPENTELEMETRY,
                            new OpenTelemetryDeploymentProcessor(this.openTelemetryConfig::get));
                })
                .build();

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        List<String> otherMetrics = new ArrayList<>();
        if (context.getCapabilityServiceSupport().hasCapability(CAPABILITY_NAME_METRICS)) {
            otherMetrics.add("WildFly Metrics");
        }
        if (context.getCapabilityServiceSupport().hasCapability(CAPABILITY_NAME_MICROMETER)) {
            otherMetrics.add("Micrometer");
        }
        if (!otherMetrics.isEmpty()) {
            if (Boolean.parseBoolean(System.getProperty("wildfly.multiple.metrics.warn", "true"))) {
                OTEL_LOGGER.multipleMetricsSystemsEnabled(String.join(",", otherMetrics));
            }
        }

        String exporter = OpenTelemetrySubsystemRegistrar.EXPORTER.resolveModelAttribute(context, model).asString();
        validateExporter(context, exporter);

        final WildFlyOpenTelemetryConfig config = new WildFlyOpenTelemetryConfig.Builder()
            .setServiceName(OpenTelemetrySubsystemRegistrar.SERVICE_NAME.resolveModelAttribute(context, model).asStringOrNull())
            .setExporter(exporter)
            .setOtlpEndpoint(OpenTelemetrySubsystemRegistrar.ENDPOINT.resolveModelAttribute(context, model).asStringOrNull())
            .setBatchDelay(OpenTelemetrySubsystemRegistrar.BATCH_DELAY.resolveModelAttribute(context, model).asLongOrNull())
            .setMaxQueueSize(OpenTelemetrySubsystemRegistrar.MAX_QUEUE_SIZE.resolveModelAttribute(context, model).asLongOrNull())
            .setMaxExportBatchSize(OpenTelemetrySubsystemRegistrar.MAX_EXPORT_BATCH_SIZE.resolveModelAttribute(context, model).asLongOrNull())
            .setExportTimeout(OpenTelemetrySubsystemRegistrar.EXPORT_TIMEOUT.resolveModelAttribute(context, model).asLongOrNull())
            .setSampler(OpenTelemetrySubsystemRegistrar.SAMPLER.resolveModelAttribute(context, model).asStringOrNull())
            .setSamplerRatio(OpenTelemetrySubsystemRegistrar.RATIO.resolveModelAttribute(context, model).asDoubleOrNull())
            .setMicroProfileTelemetryInstalled(context.hasOptionalCapability("org.wildfly.extension.microprofile.telemetry", OPENTELEMETRY_CAPABILITY, null))
            .setInjectVertx(context.hasOptionalCapability("org.wildfly.extension.vertx", OPENTELEMETRY_CAPABILITY, null))
            .build();

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
