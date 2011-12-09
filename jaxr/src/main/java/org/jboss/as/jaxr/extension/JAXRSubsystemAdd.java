/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.extension;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.jaxr.service.JAXRConfiguration;
import org.jboss.as.jaxr.service.JAXRConfigurationService;
import org.jboss.as.jaxr.service.JAXRConnectionFactoryService;
import org.jboss.as.jaxr.service.JAXRConstants;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
class JAXRSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private final JAXRConfiguration config;

    JAXRSubsystemAdd(JAXRConfiguration config) {
        this.config = config;
    }

    static ModelNode createAddSubsystemOperation() {
        final ModelNode addop = new ModelNode();
        addop.get(OP).set(ADD);
        addop.get(OP_ADDR).add(SUBSYSTEM, JAXRConstants.SUBSYSTEM_NAME);
        return addop;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (String attr : JAXRConfiguration.REQUIRED_ATTRIBUTES) {
            ModelNode node = operation.get(attr);
            JAXRWriteAttributeHandler.applyUpdateToConfig(config, attr, node);
            model.get(attr).set(node);
        }
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verifyHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_JAXR, new JAXRDependencyProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        context.addStep(new OperationStepHandler() {
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ServiceTarget serviceTarget = context.getServiceTarget();
                newControllers.add(JAXRConfigurationService.addService(serviceTarget, config, verifyHandler));
                newControllers.add(JAXRConnectionFactoryService.addService(serviceTarget, verifyHandler));
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
