/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.metrics.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.METRICS_REGISTRY_RUNTIME_CAPABILITY;
import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.WILDFLY_COLLECTOR;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.ServerService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentCompleteServiceProcessor;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.metrics.MetricCollector;
import org.wildfly.extension.metrics.MetricRegistration;
import org.wildfly.extension.metrics.MetricRegistry;
import org.wildfly.extension.metrics.WildFlyMetricRegistry;

public class DeploymentMetricService implements Service {


    private final Resource rootResource;
    private final ManagementResourceRegistration managementResourceRegistration;
    private PathAddress deploymentAddress;
    private final Supplier<MetricCollector> metricCollector;
    private Supplier<MetricRegistry> metricRegistry;
    private Supplier<Executor> managementExecutor;
    private final boolean exposeAnySubsystem;
    private final List<String> exposedSubsystems;
    private final String prefix;
    private MetricRegistration registration;

    public static void install(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit, Resource rootResource,
                               ManagementResourceRegistration managementResourceRegistration,
                               boolean exposeAnySubsystem, List<String> exposedSubsystems, String prefix) {
        PathAddress deploymentAddress = createDeploymentAddressPrefix(deploymentUnit);

        ServiceBuilder<?> sb = serviceTarget.addService(deploymentUnit.getServiceName().append("metrics"));
        Supplier<MetricCollector> metricCollector = sb.requires(WILDFLY_COLLECTOR);
        Supplier<MetricRegistry> metricRegistry = sb.requires(METRICS_REGISTRY_RUNTIME_CAPABILITY.getCapabilityServiceName());
        Supplier<Executor> managementExecutor = sb.requires(ServerService.EXECUTOR_CAPABILITY.getCapabilityServiceName());

        /*
         * The deployment metric service depends on the deployment complete service name to ensure that the metrics from
         * the deployment are collected and registered once the deployment services have all been properly installed.
         */
        sb.requires(DeploymentCompleteServiceProcessor.serviceName(deploymentUnit.getServiceName()));
        sb.setInstance(new DeploymentMetricService(rootResource, managementResourceRegistration, deploymentAddress,
                        metricCollector, metricRegistry, managementExecutor, exposeAnySubsystem, exposedSubsystems,
                        prefix))

                .install();
    }

    private DeploymentMetricService(Resource rootResource,
                                    ManagementResourceRegistration managementResourceRegistration,
                                    PathAddress deploymentAddress,
                                    Supplier<MetricCollector> metricCollector,
                                    Supplier<MetricRegistry> metricRegistry,
                                    Supplier<Executor> managementExecutor,
                                    boolean exposeAnySubsystem,
                                    List<String> exposedSubsystems,
                                    String prefix) {
        this.rootResource = rootResource;
        this.managementResourceRegistration = managementResourceRegistration;
        this.deploymentAddress = deploymentAddress;
        this.metricCollector = metricCollector;
        this.metricRegistry = metricRegistry;
        this.managementExecutor = managementExecutor;
        this.exposeAnySubsystem = exposeAnySubsystem;
        this.exposedSubsystems = exposedSubsystems;
        this.prefix = prefix;
    }

    @Override
    public void start(StartContext startContext) {
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                WildFlyMetricRegistry mainRegistry = (WildFlyMetricRegistry) metricRegistry.get();
                MetricRegistry registry = mainRegistry.addDeploymentRegistry(deploymentAddress.toCLIStyleString());

                registration = new MetricRegistration(registry);
                metricCollector.get().collectResourceMetrics(rootResource,
                        managementResourceRegistration,
                        // prepend the deployment address to the subsystem resource address
                        address -> deploymentAddress.append(address),
                        exposeAnySubsystem, exposedSubsystems, prefix,
                        registration);
                startContext.complete();
            }
        };
        try {
            managementExecutor.get().execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            startContext.asynchronous();
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        ((WildFlyMetricRegistry) metricRegistry.get()).removeDeploymentRegistry(deploymentAddress.toCLIStyleString());
    }

    private static PathAddress createDeploymentAddressPrefix(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            return PathAddress.pathAddress(DEPLOYMENT, deploymentUnit.getAttachment(Attachments.MANAGEMENT_NAME));
        } else {
            return createDeploymentAddressPrefix(deploymentUnit.getParent()).append(SUBDEPLOYMENT, deploymentUnit.getName());
        }
    }

}
