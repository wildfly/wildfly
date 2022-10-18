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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.wildfly.extension.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;
import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_COLLECTOR;

import java.util.List;
import java.util.function.Supplier;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentCompleteServiceProcessor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;

class MicrometerDeploymentService implements Service {
    private final Resource rootResource;
    private final ManagementResourceRegistration managementResourceRegistration;
    private final PathAddress deploymentAddress;
    private final Supplier<MicrometerCollector> metricCollector;
    private final boolean exposeAnySubsystem;
    private final List<String> exposedSubsystems;

    static void install(ServiceTarget serviceTarget,
                               DeploymentPhaseContext deploymentPhaseContext,
                               Resource rootResource,
                               ManagementResourceRegistration managementResourceRegistration,
                               boolean exposeAnySubsystem,
                               List<String> exposedSubsystems) {
        MICROMETER_LOGGER.processingDeployment();

        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return;
        }

        PathAddress deploymentAddress = createDeploymentAddressPrefix(deploymentUnit);

        ServiceBuilder<?> sb = serviceTarget.addService(deploymentUnit.getServiceName().append(".micrometer-metrics"));
        Supplier<MicrometerCollector> metricCollectorSupplier = sb.requires(MICROMETER_COLLECTOR);

        /*
         * The deployment metric service depends on the deployment complete service name to ensure that the metrics from
         * the deployment are collected and registered once the deployment services have all be properly installed.
         */
        sb.requires(DeploymentCompleteServiceProcessor.serviceName(deploymentUnit.getServiceName()));
        sb.setInstance(new MicrometerDeploymentService(rootResource, managementResourceRegistration, deploymentAddress,
                        metricCollectorSupplier, exposeAnySubsystem, exposedSubsystems))
                .install();
    }



    private MicrometerDeploymentService(Resource rootResource,
                                       ManagementResourceRegistration managementResourceRegistration,
                                       PathAddress deploymentAddress,
                                       Supplier<MicrometerCollector> metricCollectorSupplier,
                                       boolean exposeAnySubsystem,
                                       List<String> exposedSubsystems) {
        this.rootResource = rootResource;
        this.managementResourceRegistration = managementResourceRegistration;
        this.deploymentAddress = deploymentAddress;
        this.metricCollector = metricCollectorSupplier;
        this.exposeAnySubsystem = exposeAnySubsystem;
        this.exposedSubsystems = exposedSubsystems;
    }

    private static PathAddress createDeploymentAddressPrefix(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            return PathAddress.pathAddress(DEPLOYMENT, deploymentUnit.getAttachment(Attachments.MANAGEMENT_NAME));
        } else {
            return createDeploymentAddressPrefix(deploymentUnit.getParent()).append(SUBDEPLOYMENT, deploymentUnit.getName());
        }
    }

    @Override
    public void start(StartContext context) {
        metricCollector.get()
                .collectResourceMetrics(rootResource,
                        managementResourceRegistration,
                        // prepend the deployment address to the subsystem resource address
                        deploymentAddress::append,
                        exposeAnySubsystem,
                        exposedSubsystems);
    }

    @Override
    public void stop(StopContext context) {
    }
}
