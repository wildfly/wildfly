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

package org.jboss.as.camel.parser;

import java.util.List;

import org.jboss.as.camel.deployment.CamelContextActivationProcessor;
import org.jboss.as.camel.deployment.CamelContextCreateProcessor;
import org.jboss.as.camel.deployment.CamelContextRegistrationProcessor;
import org.jboss.as.camel.service.CamelBootstrapService;
import org.jboss.as.camel.service.CamelContextRegistryService;
import org.jboss.as.camel.service.SubsystemStateService;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * The Camel subsystem add update handler.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
final class CamelSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private final SubsystemState subsystemState;

    public CamelSubsystemAdd(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {

        // Register subsystem services
        newControllers.add(CamelBootstrapService.addService(context.getServiceTarget(), verificationHandler));
        newControllers.add(CamelContextRegistryService.addService(context.getServiceTarget(), verificationHandler));
        newControllers.add(SubsystemStateService.addService(context.getServiceTarget(), subsystemState, verificationHandler));

        // Register deployment unit processors
        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_CAMEL_CONTEXT_CREATE, new CamelContextCreateProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_CAMEL_CONTEXT_REGISTRATION, new CamelContextRegistrationProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_CAMEL_CONTEXT_ACTIVATION, new CamelContextActivationProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }

    protected boolean requiresRuntimeVerification() {
        return false;
    }
}
