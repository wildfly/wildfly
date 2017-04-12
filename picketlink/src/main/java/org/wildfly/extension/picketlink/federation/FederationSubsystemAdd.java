/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.federation;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.federation.deployment.FederationDependencyProcessor;
import org.wildfly.extension.picketlink.federation.deployment.FederationDeploymentProcessor;
import org.wildfly.extension.picketlink.logging.PicketLinkLogger;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class FederationSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public static final FederationSubsystemAdd INSTANCE = new FederationSubsystemAdd();

    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        PicketLinkLogger.ROOT_LOGGER.activatingSubsystem("Federation");

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                PicketLinkLogger.ROOT_LOGGER.trace("Installing the PicketLink Defederation Dependency Processor.");
                processorTarget.addDeploymentProcessor(FederationExtension.SUBSYSTEM_NAME, FederationDependencyProcessor.PHASE,
                    FederationDependencyProcessor.PRIORITY, new FederationDependencyProcessor());
                PicketLinkLogger.ROOT_LOGGER.trace("Installing the PicketLink Federation Deployment Processor.");
                processorTarget.addDeploymentProcessor(FederationExtension.SUBSYSTEM_NAME, FederationDeploymentProcessor.PHASE,
                    FederationDeploymentProcessor.PRIORITY, new FederationDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
