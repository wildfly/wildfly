/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web.deployment;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardWrapper;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.Locale;

/**
 * @author Emanuel Muckenhuber
 */
public class ServletDeploymentStats {

    public static final DescriptionProvider provider = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

    public static void register(final ManagementResourceRegistration registration) {

        registration.registerMetric("load-time", new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final Wrapper wrapper) {
                response.set(((StandardWrapper)wrapper).getLoadTime());
            }
        });
        registration.registerMetric("max-time", new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final Wrapper wrapper) {
                response.set(((StandardWrapper)wrapper).getMinTime());
            }
        });
        registration.registerMetric("min-time", new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final Wrapper wrapper) {
                response.set(((StandardWrapper)wrapper).getLoadTime());
            }
        });
        registration.registerMetric("processing-time", new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final Wrapper wrapper) {
                response.set(((StandardWrapper)wrapper).getProcessingTime());
            }
        });
        registration.registerMetric("request-count", new AbstractMetricsHandler() {
            @Override
            void handle(final ModelNode response, final String name, final Wrapper wrapper) {
                response.set(((StandardWrapper)wrapper).getRequestCount());
            }
        });
    }

    abstract static class AbstractMetricsHandler implements OperationStepHandler {

        abstract void handle(ModelNode response, String name, Wrapper wrapper);

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));

            final Resource web = context.readResourceFromRoot(address.subAddress(0, address.size() -1), false);
            final ModelNode subModel = web.getModel();

            final String host = subModel.require("virtual-host").asString();
            final String path = subModel.require("context-root").asString();

            final ModelNode node  = web.requireChild(address.getLastElement()).getModel();

            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final ServiceController<?> controller = context.getServiceRegistry(false).getService(WebSubsystemServices.deploymentServiceName(host, path));
                    if(controller != null) {
                        final String name = node.get("servlet-name").asString();
                        final Context webContext = Context.class.cast(controller.getValue());
                        final Wrapper wrapper = Wrapper.class.cast(webContext.findChild(name));
                        final ModelNode response = new ModelNode();
                        handle(response, address.getLastElement().getValue(), wrapper);
                        context.getResult().set(response);
                    }
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
            context.completeStep();
        }
    }

}
