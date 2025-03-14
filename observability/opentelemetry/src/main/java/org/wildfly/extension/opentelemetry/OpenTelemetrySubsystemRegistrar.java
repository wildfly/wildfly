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
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.AGGREGATION_EXPLICIT;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.ALLOWED_EXEMPLAR_FILTERS;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.ALLOWED_HISTOGRAM_AGGREGATION;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.ALLOWED_TEMPORALITY;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.BATCH_DELAY;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.DEFAULT_EXPORT_TIMEOUT;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.DEFAULT_LOGS_EXPORT_INTERVAL;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.DEFAULT_MAX_EXPORT_BATCH_SIZE;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.DEFAULT_MAX_QUEUE_SIZE;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.DEFAULT_METRICS_EXPORT_INTERVAL;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.DEFAULT_TRACE_EXPORT_INTERVAL;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.EXEMPLAR_FILTER_TRACE_BASED;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.EXPORTER_JAEGER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.EXPORTER_OTLP;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.EXPORT_INTERVAL;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_EXPORTER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_LOGS;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_METRICS;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_SAMPLER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_SPAN_PROCESSOR;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.GROUP_TLS;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.OPENTELEMETRY_CAPABILITY_NAME;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.SPAN_PROCESSOR_BATCH;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.SUBSYSTEM_PATH;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.SUBSYSTEM_RESOLVER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.TEMPORALITY_CUMULATIVE;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.VERTX_DISABLE_DNS_RESOLVER;
import static org.wildfly.extension.opentelemetry.OpenTelemetryExtensionLogger.OTEL_LOGGER;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringAllowedValuesValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
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

    private static final RuntimeCapability<Void> ELYTRON_SSL_CONTEXT_CAPABILITY =
        RuntimeCapability.Builder.of(OpenTelemetryConfigurationConstants.ELYTRON_SSL_CONTEXT_CAPABILITY_NAME, true, SSLContext.class)
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

    public static final SimpleAttributeDefinition COMPRESSION = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.COMPRESSION, ModelType.STRING, true)
            .setStability(Stability.PREVIEW)
            .setAllowExpression(true)
            .setValidator(new StringAllowedValuesValidator(OpenTelemetryConfigurationConstants.ALLOWED_COMPRESSION))
            .setRestartAllServices()
            .build();

    // TODO - TLS-related attributes, pending discussion with Elytron team
    static final UnaryServiceDescriptor<SSLContext> ELYTRON_SSL_CONTEXT =
        UnaryServiceDescriptor.of(OpenTelemetryConfigurationConstants.ELYTRON_SSL_CONTEXT_CAPABILITY_NAME, SSLContext.class);
    static final CapabilityReferenceAttributeDefinition<SSLContext> SSL_CONTEXT =
        new CapabilityReferenceAttributeDefinition.Builder<SSLContext>(OpenTelemetryConfigurationConstants.SSL_CONTEXT,
            CapabilityReference.builder(OPENTELEMETRY_CAPABILITY, ELYTRON_SSL_CONTEXT).build())
        .setRequired(false)
        .setStability(Stability.PREVIEW)
        .setAttributeGroup(GROUP_TLS)
        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SSL_REF)
        .build();
    // TODO - TLS-related attributes, pending discussion with Elytron team


    public static final SimpleAttributeDefinition TRACES_ENABLED = SimpleAttributeDefinitionBuilder
        .create(OpenTelemetryConfigurationConstants.TRACES_ENABLED, ModelType.BOOLEAN, true)
        .setStability(Stability.PREVIEW)
        .setAllowExpression(true)
        .setXmlName(OpenTelemetryConfigurationConstants.ENABLED)
        .setAttributeGroup(GROUP_SPAN_PROCESSOR)
        .setDefaultValue(new ModelNode(true))
        .setRestartAllServices()
        .build();

    public static final SimpleAttributeDefinition SPAN_PROCESSOR_TYPE = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.SPAN_PROCESSOR_TYPE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setXmlName(OpenTelemetryConfigurationConstants.TYPE)
            .setAttributeGroup(GROUP_SPAN_PROCESSOR)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(SPAN_PROCESSOR_BATCH))
            .setValidator(new StringAllowedValuesValidator(OpenTelemetryConfigurationConstants.ALLOWED_SPAN_PROCESSORS))
            .build();

    public static final SimpleAttributeDefinition TRACES_EXPORT_INTERVAL = SimpleAttributeDefinitionBuilder
            .create(OpenTelemetryConfigurationConstants.TRACES_EXPORT_INTERVAL, ModelType.LONG, true)
            .setXmlName(EXPORT_INTERVAL)
            .setStability(Stability.PREVIEW)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_SPAN_PROCESSOR)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(DEFAULT_TRACE_EXPORT_INTERVAL))
            .build();

    public static final SimpleAttributeDefinition TRACE_BATCH_DELAY = SimpleAttributeDefinitionBuilder
            .create(BATCH_DELAY, ModelType.LONG, true)
            .addFlag(AttributeAccess.Flag.ALIAS)
            .setAllowExpression(true)
            .setAttributeGroup(GROUP_SPAN_PROCESSOR)
            .setRestartAllServices()
            .setDefaultValue(new ModelNode(DEFAULT_TRACE_EXPORT_INTERVAL))
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


    // *****************************************************************************************************************
    // Tracing attributes

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

    // *****************************************************************************************************************
    // Metrics attributes
    public static final SimpleAttributeDefinition METRICS_ENABLED = SimpleAttributeDefinitionBuilder
        .create(OpenTelemetryConfigurationConstants.METRICS_ENABLED, ModelType.BOOLEAN, true)
        .setStability(Stability.PREVIEW)
        .setAllowExpression(true)
        .setXmlName(OpenTelemetryConfigurationConstants.ENABLED)
        .setAttributeGroup(GROUP_METRICS)
        .setDefaultValue(new ModelNode(true))
        .setRestartAllServices()
        .build();

    public static final SimpleAttributeDefinition METRICS_EXPORT_INTERVAL = SimpleAttributeDefinitionBuilder
        .create(OpenTelemetryConfigurationConstants.METRICS_EXPORT_INTERVAL, ModelType.LONG, true)
        .setXmlName(EXPORT_INTERVAL)
        .setStability(Stability.PREVIEW)
        .setAllowExpression(true)
        .setAttributeGroup(GROUP_METRICS)
        .setRestartAllServices()
        .setDefaultValue(new ModelNode(DEFAULT_METRICS_EXPORT_INTERVAL))
        .build();
    public static final SimpleAttributeDefinition METRICS_EXEMPLAR_FILTER = SimpleAttributeDefinitionBuilder
        .create(OpenTelemetryConfigurationConstants.EXEMPLAR_FILTER, ModelType.STRING, true)
        .setStability(Stability.PREVIEW)
        .setAllowExpression(true)
        .setAttributeGroup(GROUP_METRICS)
        .setValidator(new StringAllowedValuesValidator(ALLOWED_EXEMPLAR_FILTERS))
        .setDefaultValue(new ModelNode(EXEMPLAR_FILTER_TRACE_BASED))
        .setRestartAllServices()
        .build();
    public static final SimpleAttributeDefinition METRICS_TEMPORALITY = SimpleAttributeDefinitionBuilder
        .create(OpenTelemetryConfigurationConstants.TEMPORALITY, ModelType.STRING, true)
        .setStability(Stability.PREVIEW)
        .setAllowExpression(true)
        .setAttributeGroup(GROUP_METRICS)
        .setValidator(new StringAllowedValuesValidator(ALLOWED_TEMPORALITY))
        .setDefaultValue(new ModelNode(TEMPORALITY_CUMULATIVE))
        .setRestartAllServices()
        .build();
    public static final SimpleAttributeDefinition METRICS_HISTOGRAM_AGGREGATION = SimpleAttributeDefinitionBuilder
        .create(OpenTelemetryConfigurationConstants.HISTOGRAM_AGGREGATION, ModelType.STRING, true)
        .setStability(Stability.PREVIEW)
        .setAllowExpression(true)
        .setAttributeGroup(GROUP_METRICS)
        .setValidator(new StringAllowedValuesValidator(ALLOWED_HISTOGRAM_AGGREGATION))
        .setDefaultValue(new ModelNode(AGGREGATION_EXPLICIT))
        .setRestartAllServices()
        .build();

    // *****************************************************************************************************************
    // Log attributes
    public static final SimpleAttributeDefinition LOGS_ENABLED = SimpleAttributeDefinitionBuilder
        .create(OpenTelemetryConfigurationConstants.LOGS_ENABLED, ModelType.BOOLEAN, true)
        .setStability(Stability.PREVIEW)
        .setAllowExpression(true)
        .setXmlName(OpenTelemetryConfigurationConstants.ENABLED)
        .setAttributeGroup(GROUP_LOGS)
        .setDefaultValue(new ModelNode(true))
        .setRestartAllServices()
        .build();
    public static final SimpleAttributeDefinition LOGS_EXPORT_INTERVAL = SimpleAttributeDefinitionBuilder
        .create(OpenTelemetryConfigurationConstants.LOGS_EXPORT_INTERVAL, ModelType.LONG, true)
        .setXmlName(EXPORT_INTERVAL)
        .setStability(Stability.PREVIEW)
        .setAllowExpression(true)
        .setAttributeGroup(GROUP_LOGS)
        .setRestartAllServices()
        .setDefaultValue(new ModelNode(DEFAULT_LOGS_EXPORT_INTERVAL))
        .build();

    public static final List<AttributeDefinition> ATTRIBUTES = List.of(
        // General
        SERVICE_NAME, EXPORTER, ENDPOINT, COMPRESSION, SSL_CONTEXT,
        // Tracing
        TRACES_ENABLED, SPAN_PROCESSOR_TYPE, TRACES_EXPORT_INTERVAL, MAX_QUEUE_SIZE, MAX_EXPORT_BATCH_SIZE, EXPORT_TIMEOUT, SAMPLER, RATIO,
        // Metrics
        METRICS_ENABLED, METRICS_EXPORT_INTERVAL, METRICS_EXEMPLAR_FILTER, METRICS_TEMPORALITY, METRICS_HISTOGRAM_AGGREGATION,
        // Logs
        LOGS_ENABLED, LOGS_EXPORT_INTERVAL
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
        ManagementResourceRegistration registration = parent.registerSubsystemModel(ResourceDefinition.builder(
            ResourceRegistration.of(SUBSYSTEM_PATH), SUBSYSTEM_RESOLVER).build());
        ResourceDescriptor descriptor = ResourceDescriptor.builder(SUBSYSTEM_RESOLVER)
                .addCapability(OPENTELEMETRY_CAPABILITY)
                .addCapability(OPENTELEMETRY_CONFIG_CAPABILITY)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .addAttributes(ATTRIBUTES)
                .renameAttribute(TRACE_BATCH_DELAY, TRACES_EXPORT_INTERVAL)
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

        final String exporter = OpenTelemetrySubsystemRegistrar.EXPORTER.resolveModelAttribute(context, model).asString();
        validateExporter(context, exporter);

        final String sslRef = OpenTelemetrySubsystemRegistrar.SSL_CONTEXT.resolveModelAttribute(context, model).asString();
        final ServiceDependency<SSLContext> sslContext = SSL_CONTEXT.resolve(context, model);

        final WildFlyOpenTelemetryConfig.Builder builder = new WildFlyOpenTelemetryConfig.Builder()
            .setServiceName(OpenTelemetrySubsystemRegistrar.SERVICE_NAME.resolveModelAttribute(context, model).asStringOrNull())
            .setExporter(exporter)
            .setOtlpEndpoint(OpenTelemetrySubsystemRegistrar.ENDPOINT.resolveModelAttribute(context, model).asStringOrNull())
            .setExportTimeout(OpenTelemetrySubsystemRegistrar.EXPORT_TIMEOUT.resolveModelAttribute(context, model).asLongOrNull())
            .setMaxQueueSize(OpenTelemetrySubsystemRegistrar.MAX_QUEUE_SIZE.resolveModelAttribute(context, model).asLongOrNull())
            .setMaxExportBatchSize(OpenTelemetrySubsystemRegistrar.MAX_EXPORT_BATCH_SIZE.resolveModelAttribute(context, model).asLongOrNull())
            .setCompression(OpenTelemetrySubsystemRegistrar.COMPRESSION.resolveModelAttribute(context, model).asStringOrNull())

            .setTracesEnabled(TRACES_ENABLED.resolveModelAttribute(context, model).asBoolean(true))
            .setTracesExportInterval(OpenTelemetrySubsystemRegistrar.TRACES_EXPORT_INTERVAL.resolveModelAttribute(context, model).asLongOrNull())
            .setSampler(OpenTelemetrySubsystemRegistrar.SAMPLER.resolveModelAttribute(context, model).asStringOrNull())
            .setSamplerRatio(OpenTelemetrySubsystemRegistrar.RATIO.resolveModelAttribute(context, model).asDoubleOrNull())

            .setMetricsEnabled(METRICS_ENABLED.resolveModelAttribute(context, model).asBoolean(true))
            .setMetricsExportInterval(OpenTelemetrySubsystemRegistrar.METRICS_EXPORT_INTERVAL.resolveModelAttribute(context, model).asLongOrNull())
            .setMetricsExemplarFilter(OpenTelemetrySubsystemRegistrar.METRICS_EXEMPLAR_FILTER.resolveModelAttribute(context, model).asStringOrNull())
            .setMetricsTemporality(OpenTelemetrySubsystemRegistrar.METRICS_TEMPORALITY.resolveModelAttribute(context, model).asStringOrNull())
            .setMetricsHistogramAggregation(OpenTelemetrySubsystemRegistrar.METRICS_HISTOGRAM_AGGREGATION.resolveModelAttribute(context, model).asStringOrNull())

            .setLogsEnabled(LOGS_ENABLED.resolveModelAttribute(context, model).asBoolean(true))
            .setLogsExportInterval(OpenTelemetrySubsystemRegistrar.LOGS_EXPORT_INTERVAL.resolveModelAttribute(context, model).asLongOrNull())

            .setMicroProfileTelemetryInstalled(context.hasOptionalCapability("org.wildfly.extension.microprofile.telemetry", OPENTELEMETRY_CAPABILITY, null))
            .setInjectVertx(context.hasOptionalCapability("org.wildfly.extension.vertx", OPENTELEMETRY_CAPABILITY, null));

        // The value for SSLContext might not be available yet, so we resolve the model value here in the runtime thread,
        // but delay the retrieval of the ServiceDependency value until Service.start() has been called, at which all declared
        // dependencies have been resolved, and the final value we're after is available.
        return CapabilityServiceInstaller.builder(OPENTELEMETRY_CONFIG_CAPABILITY,
                sslContext.map(ssl -> builder.setSslContext(ssl).build()))
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
