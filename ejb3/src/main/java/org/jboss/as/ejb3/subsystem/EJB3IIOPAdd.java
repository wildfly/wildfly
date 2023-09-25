/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.ejb3.deployment.processors.EjbIIOPDeploymentUnitProcessor;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

/**
 * A {@link org.jboss.as.controller.AbstractBoottimeAddStepHandler} to handle the add operation for the Jakarta Enterprise Beans
 * IIOP service
 *
 * @author Stuart Douglas
 */
public class EJB3IIOPAdd extends AbstractBoottimeAddStepHandler {

    EJB3IIOPAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final boolean enableByDefault = EJB3IIOPResourceDefinition.ENABLE_BY_DEFAULT.resolveModelAttribute(context, model).asBoolean();
        final boolean useQualifiedName = EJB3IIOPResourceDefinition.USE_QUALIFIED_NAME.resolveModelAttribute(context, model).asBoolean();
        final IIOPSettingsService settingsService = new IIOPSettingsService(enableByDefault, useQualifiedName);
        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                ROOT_LOGGER.debug("Adding Jakarta Enterprise Beans IIOP support");
                processorTarget.addDeploymentProcessor(EJB3Extension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EJB_IIOP, new EjbIIOPDeploymentUnitProcessor(settingsService));
            }
        }, OperationContext.Stage.RUNTIME);

        context.getCapabilityServiceTarget().addCapability(EJB3IIOPResourceDefinition.EJB3_IIOP_SETTINGS_CAPABILITY).setInstance(settingsService).install();
    }
}
