/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentService;
import org.wildfly.extension.undertow.deployment.UndertowMetricsCollector;

import io.undertow.server.handlers.MetricsHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 18:35
 */
public class DeploymentServletDefinition extends SimpleResourceDefinition {
    public static final DeploymentServletDefinition INSTANCE = new DeploymentServletDefinition();

    static final SimpleAttributeDefinition SERVLET_NAME = new SimpleAttributeDefinitionBuilder("servlet-name", ModelType.STRING, false).setStorageRuntime().build();
    static final SimpleAttributeDefinition SERVLET_CLASS = new SimpleAttributeDefinitionBuilder("servlet-class", ModelType.STRING, false).setStorageRuntime().build();
    static final SimpleAttributeDefinition MAX_REQUEST_TIME = new SimpleAttributeDefinitionBuilder("max-request-time", ModelType.LONG, true).setStorageRuntime().build();
    static final SimpleAttributeDefinition MIN_REQUEST_TIME = new SimpleAttributeDefinitionBuilder("min-request-time", ModelType.LONG, true).setStorageRuntime().build();
    static final SimpleAttributeDefinition TOTAL_REQUEST_TIME = new SimpleAttributeDefinitionBuilder("total-request-time", ModelType.LONG, true).setStorageRuntime().build();
    static final SimpleAttributeDefinition REQUEST_COUNT = new SimpleAttributeDefinitionBuilder("request-count", ModelType.LONG, true).setStorageRuntime().build();
    static final SimpleListAttributeDefinition SERVLET_MAPPINGS = new SimpleListAttributeDefinition.Builder("mappings", new SimpleAttributeDefinitionBuilder("mapping", ModelType.STRING).setRequired(false).build())
            .setRequired(false)
            .setStorageRuntime()
            .build();


    private DeploymentServletDefinition() {
        super(PathElement.pathElement("servlet"),
                UndertowExtension.getResolver("deployment.servlet"));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        registration.registerReadOnlyAttribute(SERVLET_NAME, null);
        registration.registerReadOnlyAttribute(SERVLET_CLASS, null);
        registration.registerMetric(MAX_REQUEST_TIME, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final MetricsHandler.MetricResult metricResult, final ServletInfo servlet) {
                response.set((long) metricResult.getMaxRequestTime());
            }
        });
        registration.registerMetric(MIN_REQUEST_TIME, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final MetricsHandler.MetricResult metricResult, final ServletInfo servlet) {
                response.set((long) metricResult.getMinRequestTime());
            }
        });
        registration.registerMetric(TOTAL_REQUEST_TIME, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final MetricsHandler.MetricResult metricResult, final ServletInfo servlet) {
                response.set(metricResult.getTotalRequestTime());
            }
        });
        registration.registerMetric(REQUEST_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final MetricsHandler.MetricResult metricResult, final ServletInfo servlet) {
                response.set(metricResult.getTotalRequests());
            }
        });
        registration.registerMetric(SERVLET_MAPPINGS, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final MetricsHandler.MetricResult metricResult, final ServletInfo servlet) {
                for (String mapping : servlet.getMappings()) {
                    response.add(mapping);
                }
            }

            @Override
            void putDefault(ModelNode response) {
                response.setEmptyList();
            }
        });
    }

    abstract static class AbstractMetricsHandler implements OperationStepHandler {

        abstract void handle(ModelNode response, String name, MetricsHandler.MetricResult metricResult, ServletInfo infos);

        void putDefault(ModelNode response) {
            response.set(0L);
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));

            final Resource web = context.readResourceFromRoot(address.subAddress(0, address.size() - 1), false);
            final ModelNode subModel = web.getModel();

            final String host = DeploymentDefinition.VIRTUAL_HOST.resolveModelAttribute(context, subModel).asString();
            final String path = DeploymentDefinition.CONTEXT_ROOT.resolveModelAttribute(context, subModel).asString();
            final String server = DeploymentDefinition.SERVER.resolveModelAttribute(context, subModel).asString();

            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final ServiceController<?> deploymentServiceController = context.getServiceRegistry(false).getService(UndertowService.deploymentServiceName(server, host, path));
                    if (deploymentServiceController == null) {
                        return;
                    }
                    final UndertowDeploymentService deploymentService = (UndertowDeploymentService) deploymentServiceController.getService();
                    final DeploymentInfo deploymentInfo = deploymentService.getDeploymentInfoInjectedValue().getValue();
                    final UndertowMetricsCollector collector = (UndertowMetricsCollector)deploymentInfo.getMetricsCollector();

                    final String name = address.getLastElement().getValue();
                    final ServletInfo servlet = deploymentInfo.getServlets().get(name);
                    final ModelNode response = new ModelNode();
                    MetricsHandler.MetricResult result = collector != null ? collector.getMetrics(name) : null;
                    if (result == null) {
                        putDefault(response);
                    } else {
                        handle(response, name, result, servlet);
                    }
                    context.getResult().set(response);
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

}
