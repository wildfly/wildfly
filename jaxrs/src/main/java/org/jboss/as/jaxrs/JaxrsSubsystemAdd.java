/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jaxrs;


import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jaxrs.deployment.JaxrsAnnotationProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsCdiIntegrationProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsComponentDeployer;
import org.jboss.as.jaxrs.deployment.JaxrsDependencyProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsIntegrationProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsScanningProcessor;
import org.jboss.as.jaxrs.deployment.JaxrsSpringProcessor;
import org.jboss.as.jaxrs.logging.JaxrsLogger;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 * The jaxrs subsystem add update handler.
 *
 * @author Stuart Douglas
 */
class JaxrsSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final JaxrsSubsystemAdd INSTANCE = new JaxrsSubsystemAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    protected void performBoottime(final OperationContext context, ModelNode operation, Resource resource) {

        final ServiceTarget serviceTarget = context.getServiceTarget();
        JaxrsLogger.JAXRS_LOGGER.resteasyVersion(ResteasyDeployment.class.getPackage().getImplementationVersion());
        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_JAXRS_ANNOTATIONS, new JaxrsAnnotationProcessor());
                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_JAXRS_SPRING, new JaxrsSpringProcessor(serviceTarget));
                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_JAXRS, new JaxrsDependencyProcessor());
                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JAXRS_SCANNING, new JaxrsScanningProcessor());
                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JAXRS_COMPONENT, new JaxrsComponentDeployer());
                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JAXRS_CDI_INTEGRATION, new JaxrsCdiIntegrationProcessor());
                processorTarget.addDeploymentProcessor(JaxrsExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_JAXRS_DEPLOYMENT, new JaxrsIntegrationProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
