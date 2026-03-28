/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.prometheus;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

import io.prometheus.metrics.expositionformats.OpenMetricsTextFormatWriter;
import io.prometheus.metrics.expositionformats.PrometheusProtobufWriter;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.micrometer.MicrometerConfigurationConstants;
import org.wildfly.extension.micrometer.MicrometerExtensionLogger;
import org.wildfly.extension.micrometer.MicrometerSubsystemRegistrar;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

public class PrometheusRegistryDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator {
    private static final Stability FEATURE_STABILITY = Stability.COMMUNITY;

    public static final String NAME = "prometheus";
    public static final PathElement PATH = PathElement.pathElement("registry", NAME);
    public static final ResourceRegistration RESOURCE_REGISTRATION = ResourceRegistration.of(PATH, FEATURE_STABILITY);

    public static final String HTTP_EXTENSIBILITY_CAPABILITY = "org.wildfly.management.http.extensible";
    public static final SimpleAttributeDefinition CONTEXT = SimpleAttributeDefinitionBuilder
            .create(MicrometerConfigurationConstants.CONTEXT, ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final AttributeDefinition SECURITY_ENABLED = SimpleAttributeDefinitionBuilder
            .create(MicrometerConfigurationConstants.SECURITY_ENABLED, ModelType.BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .build();
    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(CONTEXT, SECURITY_ENABLED);

    // Formats supported in io.prometheus.metrics.expositionformats.ExpositionFormats
    private static final MediaType PROMETHEUS_TEXT_MEDIA_TYPE = MediaType.parse(PrometheusTextFormatWriter.CONTENT_TYPE);
    private static final MediaType OPENMETRICS_TEXT_MEDIA_TYPE = MediaType.parse(OpenMetricsTextFormatWriter.CONTENT_TYPE);
    private static final MediaType PROMETHEUS_PROTOBUF_MEDIA_TYPE = MediaType.parse(PrometheusProtobufWriter.CONTENT_TYPE);

    private final WildFlyCompositeRegistry wildFlyRegistry;

    public PrometheusRegistryDefinitionRegistrar(WildFlyCompositeRegistry wildFlyRegistry) {
        this.wildFlyRegistry = wildFlyRegistry;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {

        ResourceDescriptor description = ResourceDescriptor.builder(MicrometerSubsystemRegistrar.RESOLVER.createChildResolver(PATH))
            .addAttributes(ATTRIBUTES)
            .withOperationTransformation(ModelDescriptionConstants.ADD, new AddHandler())
            .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
            .build();
        ManagementResourceRegistration resourceRegistration = parent.registerSubModel(
                ResourceDefinition.builder(RESOURCE_REGISTRATION, description.getResourceDescriptionResolver()).build());
        if (resourceRegistration != null) {
            resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.required("io.prometheus"));
        }
        ManagementResourceRegistrar.of(description).register(resourceRegistration);

        return resourceRegistration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String serviceContext = CONTEXT.resolveModelAttribute(context, model).asString();
        boolean securityEnabled = SECURITY_ENABLED.resolveModelAttribute(context, model).asBoolean();

        return ServiceInstaller.builder(ServiceDependency.on(HTTP_EXTENSIBILITY_CAPABILITY, ExtensibleHttpManagement.class))
                .onStart(ehm -> {
                    WildFlyPrometheusRegistry prometheusRegistry = new WildFlyPrometheusRegistry();
                    wildFlyRegistry.add(prometheusRegistry);
                    ehm.addManagementHandler(serviceContext, securityEnabled,
                            exchange -> handleRequest(exchange, prometheusRegistry)
                    );
                })
                .startWhen(StartWhen.INSTALLED)
                .build();
    }

    private void handleRequest(HttpServerExchange exchange, WildFlyPrometheusRegistry prometheusRegistry) {
        HeaderValues headerValues = exchange.getRequestHeaders().get(Headers.ACCEPT);
        List<MediaType> mediaTypes = headerValues != null ? getSortedMediaTypes(String.join(", ", headerValues)) : List.of();
        boolean sent = false;
        for (MediaType mediaType : mediaTypes) {
            MediaType metricsMediaType = getMetricsMediaType(mediaType);
            if (metricsMediaType != null) {
                sendMetrics(metricsMediaType, prometheusRegistry, exchange);
                sent = true;
                break;
            }
        }
        if (!sent) {
            if (headerValues== null || headerValues.isEmpty()) {
                sendMetrics(PROMETHEUS_TEXT_MEDIA_TYPE, prometheusRegistry, exchange);
            } else {
                exchange.setStatusCode(StatusCodes.UNSUPPORTED_MEDIA_TYPE);
                exchange.getResponseSender().send("Unsupported Media Type");
            }
        }
    }

    private static MediaType getMetricsMediaType(MediaType mediaType) {
        MediaType metricsMediaType = null;
        if (PROMETHEUS_TEXT_MEDIA_TYPE.matches(mediaType.type(), mediaType.subtype())) {
            metricsMediaType = PROMETHEUS_TEXT_MEDIA_TYPE;
        } else if (OPENMETRICS_TEXT_MEDIA_TYPE.matches(mediaType.type(), mediaType.subtype())) {
            metricsMediaType = OPENMETRICS_TEXT_MEDIA_TYPE;
        } else if (PROMETHEUS_PROTOBUF_MEDIA_TYPE.matches(mediaType.type(), mediaType.subtype())) {
            metricsMediaType = PROMETHEUS_PROTOBUF_MEDIA_TYPE;
        }
        return metricsMediaType;
    }

    private void sendMetrics(MediaType mediaType, WildFlyPrometheusRegistry prometheusRegistry, HttpServerExchange exchange) {
        String metrics = prometheusRegistry.scrape(mediaType.toString());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, mediaType.toString());
        exchange.getResponseSender().send(metrics);
    }

    private List<MediaType> getSortedMediaTypes(String acceptHeaderValues) {
        if (acceptHeaderValues == null || acceptHeaderValues.isEmpty()) {
            return List.of(PROMETHEUS_TEXT_MEDIA_TYPE);
        } else {
            return parseMediaType(acceptHeaderValues);
        }
    }

    private static List<MediaType> parseMediaType(String acceptHeaderValues) {
        if (acceptHeaderValues == null || acceptHeaderValues.isEmpty()) {
            return List.of(new MediaType(MediaType.WILDCARD, MediaType.WILDCARD, 1.0));
        }

        return Arrays.stream(acceptHeaderValues.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(MediaType::parse).sorted()
                .toList();
    }

    private static class AddHandler implements UnaryOperator<OperationStepHandler> {
        @Override
        public OperationStepHandler apply(OperationStepHandler operationStepHandler) {
            return (operationContext, operation) -> {
                if (operationContext.getProcessType().isHostController()) {
                    throw MicrometerExtensionLogger.MICROMETER_LOGGER.prometheusNotSupportedOnHostControllers();
                }
                operationStepHandler.execute(operationContext, operation);

                ModelNode model = operationContext.readResource(PathAddress.EMPTY_ADDRESS, false).getModel();
                String context = CONTEXT.resolveModelAttribute(operationContext, model).asString();

                if (context.startsWith("/")) {
                    context = context.substring(1);
                }

                operationContext.registerCapability(
                    RuntimeCapability.Builder.of("org.wildfly.management.context", true).build()
                        .fromBaseCapability(context)
                );

            };
        }
    }
}
