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
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.service.ServiceController;

import java.util.Locale;

/**
 * @author Emanuel Muckenhuber
 */
public class ServletDeploymentStats {

    static final DescriptionProvider provider = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

    public static void register(final ModelNodeRegistration registration) {

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

    abstract static class AbstractMetricsHandler implements NewStepHandler {

        abstract void handle(ModelNode response, String name, Wrapper wrapper);

        @Override
        public void execute(final NewOperationContext context, final ModelNode operation) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
            final String deploymentName = address.getElement(address.size() -3).getValue();
            final ModelNode node  = context.readModel(PathAddress.EMPTY_ADDRESS);
            context.addStep(new NewStepHandler() {
                @Override
                public void execute(final NewOperationContext context, final ModelNode operation) throws OperationFailedException {
                    final ServiceController<?> controller = context.getServiceRegistry(false).getRequiredService(WebSubsystemServices.JBOSS_WEB.append(deploymentName));
                    if(controller != null) {
                        final String name = node.get("servlet-name").asString();
                        final Context webContext = Context.class.cast(controller.getValue());
                        final Wrapper wrapper = Wrapper.class.cast(webContext.findChild(name));
                        handle(context.getResult(), address.getLastElement().getValue(), wrapper);
                    }
                    context.completeStep();
                }
            }, NewOperationContext.Stage.RUNTIME);
            context.completeStep();
        }
    }

}
