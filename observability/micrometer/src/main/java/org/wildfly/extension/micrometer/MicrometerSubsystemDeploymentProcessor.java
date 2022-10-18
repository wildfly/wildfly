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

package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;
import static org.wildfly.extension.micrometer.MicrometerSubsystemExtension.WELD_CAPABILITY_NAME;

import java.util.List;
import java.util.function.Supplier;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentModelUtils;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.wildfly.extension.micrometer.api.MicrometerCdiExtension;
import org.wildfly.extension.micrometer.metrics.WildFlyRegistry;

class MicrometerSubsystemDeploymentProcessor implements DeploymentUnitProcessor {
    private final boolean exposeAnySubsystem;
    private final List<String> exposedSubsystems;
    private final Supplier<WildFlyRegistry> registrySupplier;

    MicrometerSubsystemDeploymentProcessor(boolean exposeAnySubsystem, List<String> exposedSubsystems,
                                                  Supplier<WildFlyRegistry> registrySupplier) {
        this.exposeAnySubsystem = exposeAnySubsystem;
        this.exposedSubsystems = exposedSubsystems;
        this.registrySupplier = registrySupplier;
    }

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();

        MicrometerDeploymentService.install(deploymentPhaseContext.getServiceTarget(),
                deploymentPhaseContext,
                deploymentUnit.getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE),
                deploymentUnit.getAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT),
                exposeAnySubsystem,
                exposedSubsystems);

        registerCdiExtension(deploymentUnit);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void registerCdiExtension(DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        try {
            CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);


            final WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
            if (!weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                // Jakarta RESTful Web Services require Jakarta Contexts and Dependency Injection. Without Jakarta
                // Contexts and Dependency Injection, there's no integration needed
                MICROMETER_LOGGER.noCdiDeployment();
                return;
            }
            WildFlyRegistry registry = registrySupplier.get();
            if (registry == null) {
                throw new DeploymentUnitProcessingException(new IllegalStateException());
            }

            weldCapability.registerExtensionInstance(new MicrometerCdiExtension(registry), deploymentUnit);
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            //We should not be here since the subsystem depends on weld capability. Just in case ...
            MICROMETER_LOGGER.deploymentRequiresCapability(deploymentUnit.getName(), WELD_CAPABILITY_NAME);
        }
    }
}
