/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.faulttolerance.deployment;

import static org.wildfly.extension.microprofile.faulttolerance.MicroProfileFaultToleranceLogger.ROOT_LOGGER;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.smallrye.faulttolerance.FaultToleranceExtension;
import io.smallrye.faulttolerance.metrics.MetricsIntegration;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.Capabilities;
import org.jboss.as.weld.WeldCapability;

/**
 * This {@link DeploymentUnitProcessor} registers required CDI portable extension that adds support
 * for MP Fault Tolerance interceptor bindings. Moreover, it specifies which metrics provider to use according to
 * metrics integrations available at runtime (MP Metrics, Micrometer, or no metrics).
 *
 * @author Radoslav Husar
 */
public class MicroProfileFaultToleranceDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!MicroProfileFaultToleranceMarker.isMarked(deploymentUnit)) {
            return;
        }

        // Weld Extension
        CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

        WeldCapability weldCapability;
        try {
            weldCapability = support.getCapabilityRuntimeAPI(Capabilities.WELD_CAPABILITY_NAME, WeldCapability.class);
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw new IllegalStateException();
        }

        // Configure which metrics provider to use
        Set<String> registeredSubsystems = deploymentUnit.getAttachment(Attachments.REGISTERED_SUBSYSTEMS);

        Set<MetricsIntegration> metricsIntegrations = new HashSet<>();

        if (registeredSubsystems.contains("microprofile-telemetry")) {
            metricsIntegrations.add(MetricsIntegration.OPENTELEMETRY);
        }

        if (registeredSubsystems.contains("micrometer")) {
            metricsIntegrations.add(MetricsIntegration.MICROMETER);
        }

        if (metricsIntegrations.isEmpty()) {
            metricsIntegrations.add(MetricsIntegration.NOOP);
        }

        ROOT_LOGGER.metricsProvider(metricsIntegrations.stream().map(Enum::name).collect(Collectors.toSet()));

        weldCapability.registerExtensionInstance(new FaultToleranceExtension(metricsIntegrations), deploymentUnit);
    }
}
