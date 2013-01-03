/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardWrapper;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 18:35
 */
public class WebDeploymentServletDefinition extends SimpleResourceDefinition {
    public static final WebDeploymentServletDefinition INSTANCE = new WebDeploymentServletDefinition();

    protected static final SimpleAttributeDefinition LOAD_TIME = new SimpleAttributeDefinitionBuilder(Constants.LOAD_TIME, ModelType.LONG, true).build();
    protected static final SimpleAttributeDefinition MAX_TIME = new SimpleAttributeDefinitionBuilder(Constants.MAX_TIME, ModelType.LONG, true).build();
    protected static final SimpleAttributeDefinition MIN_TIME = new SimpleAttributeDefinitionBuilder(Constants.MIN_TIME, ModelType.LONG, true).build();
    protected static final SimpleAttributeDefinition PROCESSING_TIME = new SimpleAttributeDefinitionBuilder(Constants.PROCESSING_TIME, ModelType.LONG, true).build();
    protected static final SimpleAttributeDefinition REQUEST_COUNT = new SimpleAttributeDefinitionBuilder(Constants.REQUEST_COUNT, ModelType.INT, true).build();


    private WebDeploymentServletDefinition() {
        super(PathElement.pathElement("servlet"),
                WebExtension.getResourceDescriptionResolver("deployment.servlet"));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        registration.registerMetric(LOAD_TIME, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final StandardWrapper wrapper) {
                response.set(wrapper.getLoadTime());
            }
        });
        registration.registerMetric(MAX_TIME, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final StandardWrapper wrapper) {
                response.set(wrapper.getMinTime());
            }
        });
        registration.registerMetric(MIN_TIME, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final StandardWrapper wrapper) {
                response.set(wrapper.getLoadTime());
            }
        });
        registration.registerMetric(PROCESSING_TIME, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final StandardWrapper wrapper) {
                response.set(wrapper.getProcessingTime());
            }
        });
        registration.registerMetric(REQUEST_COUNT, new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final StandardWrapper wrapper) {
                response.set(wrapper.getRequestCount());
            }
        });
    }

    abstract static class AbstractMetricsHandler implements OperationStepHandler {

        abstract void handle(ModelNode response, String name, StandardWrapper wrapper);

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));

            final Resource web = context.readResourceFromRoot(address.subAddress(0, address.size() - 1), false);
            final ModelNode subModel = web.getModel();

            final String host = subModel.require("virtual-host").asString();
            final String path = subModel.require("context-root").asString();

            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final ServiceController<?> controller = context.getServiceRegistry(false).getService(WebSubsystemServices.deploymentServiceName(host, path));
                    if (controller != null) {
                        final String name = address.getLastElement().getValue();
                        final Context webContext = Context.class.cast(controller.getValue());
                        final Wrapper wrapper = Wrapper.class.cast(webContext.findChild(name));
                        final ModelNode response = new ModelNode();
                        handle(response, name, (StandardWrapper) wrapper);
                        context.getResult().set(response);
                    }
                    context.stepCompleted();
                }
            }, OperationContext.Stage.RUNTIME);
            context.stepCompleted();
        }
    }

}
