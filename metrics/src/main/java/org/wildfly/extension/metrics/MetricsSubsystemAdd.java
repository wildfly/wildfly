/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.metrics;

import static org.jboss.as.controller.OperationContext.Stage.RUNTIME;
import static org.jboss.as.controller.OperationContext.Stage.VERIFY;
import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES_MICROPROFILE_METRICS;
import static org.jboss.as.server.deployment.Phase.INSTALL;
import static org.jboss.as.server.deployment.Phase.POST_MODULE_MICROPROFILE_METRICS;
import static org.wildfly.extension.metrics.MetricsSubsystemDefinition.WILDFLY_COLLECTOR_SERVICE;
import static org.wildfly.extension.metrics.WildFlyMetricRegistryService.WILDFLY_METRIC_REGISTRY_SERVICE;
import static org.wildfly.extension.metrics._private.MetricsLogger.LOGGER;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.metrics.deployment.DependencyProcessor;
import org.wildfly.extension.metrics.deployment.DeploymentMetricProcessor;
import org.wildfly.extension.metrics.internal.MetricCollector;
import org.wildfly.extension.metrics.internal.WildFlyMetricRegistry;
import org.wildfly.extension.metrics.internal.jmx.JmxMetricCollector;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
class MetricsSubsystemAdd extends AbstractBoottimeAddStepHandler {


    MetricsSubsystemAdd() {
        super(MetricsSubsystemDefinition.ATTRIBUTES);
    }

    static final MetricsSubsystemAdd INSTANCE = new MetricsSubsystemAdd();

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performBoottime(context, operation, model);

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                // TODO use proper Phase constants for these DUPs
                processorTarget.addDeploymentProcessor(MetricsExtension.SUBSYSTEM_NAME, DEPENDENCIES, DEPENDENCIES_MICROPROFILE_METRICS - 1, new DependencyProcessor());
                processorTarget.addDeploymentProcessor(MetricsExtension.SUBSYSTEM_NAME, INSTALL, POST_MODULE_MICROPROFILE_METRICS - 1, new DeploymentMetricProcessor());
            }
        }, RUNTIME);

        boolean securityEnabled = MetricsSubsystemDefinition.SECURITY_ENABLED.resolveModelAttribute(context, model).asBoolean();
        List<String> exposedSubsystems = MetricsSubsystemDefinition.EXPOSED_SUBSYSTEMS.unwrap(context, model);
        String prefix = MetricsSubsystemDefinition.PREFIX.resolveModelAttribute(context, model).asStringOrNull();

        WildFlyMetricRegistryService.install(context);
        MetricsCollectorService.install(context, exposedSubsystems, prefix);
        MetricsContextService.install(context, securityEnabled);
        // delay the registration of the metrics in the VERIFY stage so that all resources
        // created during the RUNTIME phase will have been registered in the MRR.
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext operationContext, ModelNode modelNode) {
                ServiceController<?> serviceController = context.getServiceRegistry(false).getService(WILDFLY_COLLECTOR_SERVICE);
                MetricCollector metricCollector = MetricCollector.class.cast(serviceController.getValue());

                ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
                Resource rootResource = context.readResourceFromRoot(EMPTY_ADDRESS);
                metricCollector.collectResourceMetrics(rootResource, rootResourceRegistration, Function.identity());

                ServiceController<?> metricRegistryServiceController = context.getServiceRegistry(false).getService(WILDFLY_METRIC_REGISTRY_SERVICE);
                WildFlyMetricRegistry wildFlyMetricRegistry = WildFlyMetricRegistry.class.cast(metricRegistryServiceController.getValue());
                JmxMetricCollector jmxMetricCollector = new JmxMetricCollector(wildFlyMetricRegistry);
                try {
                    jmxMetricCollector.init();
                } catch (IOException e) {
                    throw LOGGER.failedInitializeJMXRegistrar(e);
                }
            }
        }, VERIFY);
        LOGGER.activatingSubsystem();
    }


}
