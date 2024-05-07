/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.metrics.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.METRICS_REGISTRY_RUNTIME_CAPABILITY;
import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.WILDFLY_COLLECTOR;

import java.util.List;
import java.util.function.Supplier;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentCompleteServiceProcessor;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.metrics.MetricCollector;
import org.wildfly.extension.metrics.MetricRegistration;
import org.wildfly.extension.metrics.MetricRegistry;
import org.wildfly.service.AsyncServiceBuilder.Async;
import org.wildfly.subsystem.service.AsyncServiceBuilder;

public class DeploymentMetricService implements Service {


    private final Resource rootResource;
    private final ManagementResourceRegistration managementResourceRegistration;
    private PathAddress deploymentAddress;
    private final Supplier<MetricCollector> metricCollector;
    private Supplier<MetricRegistry> metricRegistry;
    private final boolean exposeAnySubsystem;
    private final List<String> exposedSubsystems;
    private final String prefix;
    private MetricRegistration registration;

    public static void install(RequirementServiceTarget serviceTarget, DeploymentUnit deploymentUnit, Resource rootResource, ManagementResourceRegistration managementResourceRegistration, boolean exposeAnySubsystem, List<String> exposedSubsystems, String prefix) {
        PathAddress deploymentAddress = createDeploymentAddressPrefix(deploymentUnit);

        ServiceBuilder<?> sb = new AsyncServiceBuilder<>(serviceTarget.addService(), Async.START_ONLY);
        Supplier<MetricCollector> metricCollector = sb.requires(WILDFLY_COLLECTOR);
        Supplier<MetricRegistry> metricRegistry = sb.requires(METRICS_REGISTRY_RUNTIME_CAPABILITY.getCapabilityServiceName());

        /*
         * The deployment metric service depends on the deployment complete service name to ensure that the metrics from
         * the deployment are collected and registered once the deployment services have all been properly installed.
         */
        sb.requires(DeploymentCompleteServiceProcessor.serviceName(deploymentUnit.getServiceName()));
        sb.setInstance(new DeploymentMetricService(rootResource, managementResourceRegistration, deploymentAddress, metricCollector, metricRegistry,
                exposeAnySubsystem, exposedSubsystems, prefix))
                .install();
    }

    private DeploymentMetricService(Resource rootResource, ManagementResourceRegistration managementResourceRegistration, PathAddress deploymentAddress,
                                    Supplier<MetricCollector> metricCollector, Supplier<MetricRegistry> metricRegistry,
                                    boolean exposeAnySubsystem, List<String> exposedSubsystems, String prefix) {
        this.rootResource = rootResource;
        this.managementResourceRegistration = managementResourceRegistration;
        this.deploymentAddress = deploymentAddress;
        this.metricCollector = metricCollector;
        this.metricRegistry = metricRegistry;
        this.exposeAnySubsystem = exposeAnySubsystem;
        this.exposedSubsystems = exposedSubsystems;
        this.prefix = prefix;
    }

    @Override
    public void start(StartContext startContext) {
        registration = new MetricRegistration(metricRegistry.get());
        metricCollector.get().collectResourceMetrics(rootResource,
                managementResourceRegistration,
                // prepend the deployment address to the subsystem resource address
                address -> deploymentAddress.append(address),
                exposeAnySubsystem, exposedSubsystems, prefix,
                registration);
    }

    @Override
    public void stop(StopContext stopContext) {
        registration.unregister();
    }

    private static PathAddress createDeploymentAddressPrefix(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            return PathAddress.pathAddress(DEPLOYMENT, deploymentUnit.getAttachment(Attachments.MANAGEMENT_NAME));
        } else {
            return createDeploymentAddressPrefix(deploymentUnit.getParent()).append(SUBDEPLOYMENT, deploymentUnit.getName());
        }
    }

}
