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
package org.jboss.as.provision.parser;

import static org.jboss.as.provision.ProvisionLogger.LOGGER;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.provision.service.AbstractResolverService;
import org.jboss.as.provision.service.EnvironmentService;
import org.jboss.as.provision.service.RepositoryService;
import org.jboss.as.provision.service.ResourceProvisionerService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * Provision subsystem operation handler.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 03-May-2013
 */
class ProvisionSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private final SubsystemState subsystemState;

    ProvisionSubsystemAdd(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        LOGGER.infoActivatingSubsystem();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                // Add the resource provisioning services
                newControllers.add(AbstractResolverService.addService(serviceTarget));
                newControllers.add(EnvironmentService.addService(serviceTarget));
                newControllers.add(RepositoryService.addService(serviceTarget, subsystemState));
                newControllers.add(ResourceProvisionerService.addService(serviceTarget));

                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
