/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.telemetry;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.telemetry.MicroProfileTelemetryExtensionLogger.MPTEL_LOGGER;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.wildfly.extension.microprofile.telemetry.api.MicroProfileTelemetryCdiExtension;

public class MicroProfileTelemetryDeploymentProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        try {
            final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
            final WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
            if (weldCapability != null && !weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                MPTEL_LOGGER.debug("The deployment does not have Jakarta Contexts and Dependency Injection enabled. " +
                        "Skipping MicroProfile Telemetry integration.");
            } else {
                final OpenTelemetryConfig serverConfig =
                        (OpenTelemetryConfig) support.getCapabilityRuntimeAPI("org.wildfly.extension.opentelemetry.config",
                                Supplier.class).get();
                Map<String, String> properties = new HashMap<>(serverConfig.properties());
                properties.put("otel.service.name", getServiceName(deploymentUnit));

                weldCapability.registerExtensionInstance(new MicroProfileTelemetryCdiExtension(properties),
                        deploymentUnit);
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw MPTEL_LOGGER.deploymentRequiresCapability(deploymentPhaseContext.getDeploymentUnit().getName(),
                    WELD_CAPABILITY_NAME);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private String getServiceName(DeploymentUnit deploymentUnit) {
        String serviceName = deploymentUnit.getServiceName().getSimpleName();
        if (null != deploymentUnit.getParent()) {
            serviceName = deploymentUnit.getParent().getServiceName().getSimpleName() + "!" + serviceName;
        }
        return serviceName;
    }
}
