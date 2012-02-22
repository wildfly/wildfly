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

package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
* @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
*/
class LoggingSubsystemAdd extends AbstractAddStepHandler {

    static final LoggingSubsystemAdd ADD_INSTANCE = new LoggingSubsystemAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        model.get(CommonAttributes.LOGGER).setEmptyObject();
        model.get(CommonAttributes.ASYNC_HANDLER).setEmptyObject();
        model.get(CommonAttributes.CONSOLE_HANDLER).setEmptyObject();
        model.get(CommonAttributes.CUSTOM_HANDLER).setEmptyObject();
        model.get(CommonAttributes.FILE_HANDLER).setEmptyObject();
        model.get(CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER).setEmptyObject();
        model.get(CommonAttributes.SIZE_ROTATING_FILE_HANDLER).setEmptyObject();
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(final DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_LOGGING_CONFIG, LoggingConfigurationProcessor.INSTANCE);
            }
        }, Stage.RUNTIME);
    }

    static ModelNode createOperation(ModelNode address) {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        return subsystem;
    }
}
