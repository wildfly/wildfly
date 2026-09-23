/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.opentelemetry.OpenTelemetryExtensionLogger.OTEL_LOGGER;

import java.util.function.Supplier;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.resources.Resource;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.wildfly.extension.opentelemetry.api.DeploymentOpenTelemetry;
import org.wildfly.extension.opentelemetry.api.OpenTelemetryCdiExtension;

class OpenTelemetryDeploymentProcessor implements DeploymentUnitProcessor {
    private static final AttachmentKey<DeploymentMetricHandle> METRIC_HANDLE_KEY =
            AttachmentKey.create(DeploymentMetricHandle.class);

    private final Supplier<OpenTelemetryService> openTelemetryService;

    public OpenTelemetryDeploymentProcessor(Supplier<OpenTelemetryService> openTelemetryService) {
        this.openTelemetryService = openTelemetryService;
    }

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        OTEL_LOGGER.debug("OpenTelemetry Subsystem is processing deployment");

        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        try {
            final WeldCapability weldCapability = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)
                    .getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
            if (!weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                OTEL_LOGGER.debug("The deployment does not have Jakarta Contexts and Dependency Injection enabled. Skipping OpenTelemetry integration.");
                return;
            }

            OpenTelemetryService service = openTelemetryService.get();
            OpenTelemetry serverOtel = service.getOpenTelemetry();
            String deploymentName = getDeploymentName(deploymentUnit);

            // Create deployment-isolated meter provider with DELTA temporality
            DeploymentMetricReader reader = new DeploymentMetricReader(AggregationTemporality.DELTA);

            // Build deployment resource (merges server resource with deployment-specific attributes)
            Resource deploymentResource = buildDeploymentResource(serverOtel, deploymentName, deploymentUnit);

            SdkMeterProvider deploymentMeter = SdkMeterProvider.builder()
                    .setResource(deploymentResource)
                    .registerMetricReader(reader)
                    .build();

            // Register with server for aggregation
            AggregatingMetricExporter.DeploymentMetricReaderHandle handle =
                    new AggregatingMetricExporter.DeploymentMetricReaderHandle(reader, deploymentName);
            service.registerDeploymentMetricReader(deploymentName, handle);

            // Store for cleanup on undeploy
            deploymentUnit.putAttachment(METRIC_HANDLE_KEY, new DeploymentMetricHandle(deploymentName, deploymentMeter));

            // Create wrapped OpenTelemetry for this deployment
            DeploymentOpenTelemetry deploymentOtel = new DeploymentOpenTelemetry(
                    serverOtel.getTracerProvider(),
                    serverOtel.getLogsBridge(),
                    deploymentMeter,
                    serverOtel.getPropagators()
            );

            // Register CDI extension with wrapped instance
            weldCapability.registerExtensionInstance(
                    new OpenTelemetryCdiExtension(deploymentOtel), deploymentUnit);
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw OTEL_LOGGER.deploymentRequiresCapability(deploymentPhaseContext.getDeploymentUnit().getName(),
                    WELD_CAPABILITY_NAME);
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        DeploymentMetricHandle handle = deploymentUnit.getAttachment(METRIC_HANDLE_KEY);

        if (handle != null) {
            // Unregister from server tracking
            openTelemetryService.get().unregisterDeploymentMetricReader(handle.deploymentName);

            // Shutdown the meter provider
            handle.meterProvider.shutdown();
        }
    }

    private String getDeploymentName(DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().getCanonicalName();
    }

    private String getServiceName(DeploymentUnit deploymentUnit) {
        String serviceName = deploymentUnit.getServiceName().getSimpleName();
        if (null != deploymentUnit.getParent()) {
            serviceName = deploymentUnit.getParent().getServiceName().getSimpleName() + "!" + serviceName;
        }
        return serviceName;
    }

    private Resource buildDeploymentResource(OpenTelemetry serverOtel, String deploymentName, DeploymentUnit deploymentUnit) {
        // Start with default resource
        Resource serverResource = Resource.getDefault();

        // Build deployment-specific attributes
        AttributesBuilder deploymentAttrs = Attributes.builder()
                .put(AttributeKey.stringKey("deployment.name"), deploymentName)
                .put(AttributeKey.stringKey("service.name"), getServiceName(deploymentUnit));

        // Merge with server resource
        return serverResource.merge(Resource.create(deploymentAttrs.build()));
    }

    private static class DeploymentMetricHandle {
        final String deploymentName;
        final SdkMeterProvider meterProvider;

        DeploymentMetricHandle(String deploymentName, SdkMeterProvider meterProvider) {
            this.deploymentName = deploymentName;
            this.meterProvider = meterProvider;
        }
    }
}
