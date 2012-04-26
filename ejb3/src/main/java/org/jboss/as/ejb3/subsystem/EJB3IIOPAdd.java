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
package org.jboss.as.ejb3.subsystem;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ejb3.deployment.processors.EjbIIOPDeploymentUnitProcessor;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.ENABLE_BY_DEFAULT;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.IIOP;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.USE_QUALIFIED_NAME;

/**
 * A {@link org.jboss.as.controller.AbstractBoottimeAddStepHandler} to handle the add operation for the EJB
 * IIOP service
 *
 * @author Stuart Douglas
 */
public class EJB3IIOPAdd extends AbstractBoottimeAddStepHandler {

    static final EJB3IIOPAdd INSTANCE = new EJB3IIOPAdd();

    private EJB3IIOPAdd() {
    }

    static ModelNode create(final boolean enableByDefault, final boolean useQualifiedName) {
        // set the address for this operation
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
        address.add(SERVICE, IIOP);

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);

        operation.get(ENABLE_BY_DEFAULT).set(enableByDefault);
        operation.get(USE_QUALIFIED_NAME).set(useQualifiedName);

        return operation;
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final Boolean enableByDefault = EJB3IIOPResourceDefinition.ENABLE_BY_DEFAULT.resolveModelAttribute(context, model).asBoolean();
        final Boolean useQualifiedName = EJB3IIOPResourceDefinition.USE_QUALIFIED_NAME.resolveModelAttribute(context, model).asBoolean();
        final IIOPSettingsService settingsService = new IIOPSettingsService(enableByDefault, useQualifiedName);
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                ROOT_LOGGER.debug("Adding EJB IIOP support");
                    processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_IIOP, new EjbIIOPDeploymentUnitProcessor(settingsService));
            }
        }, OperationContext.Stage.RUNTIME);

        newControllers.add(context.getServiceTarget().addService(IIOPSettingsService.SERVICE_NAME, settingsService).install());
    }



    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.get(ENABLE_BY_DEFAULT).set(operation.require(ENABLE_BY_DEFAULT).asString());
        model.get(USE_QUALIFIED_NAME).set(operation.require(USE_QUALIFIED_NAME).asString());
    }
}
