/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.connector.deployers.datasource.DefaultDataSourceBindingProcessor;
import org.jboss.as.connector.deployers.datasource.DefaultDataSourceResourceReferenceProcessor;
import org.jboss.as.connector.deployers.ds.DsDeploymentActivator;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

/**
 * Handler for adding the datasource subsystem.
 *
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author John Bailey
 */
class DataSourcesSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final DataSourcesSubsystemAdd INSTANCE = new DataSourcesSubsystemAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {

        final DsDeploymentActivator dsDeploymentActivator = new DsDeploymentActivator();

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                dsDeploymentActivator.activateProcessors(processorTarget);
                processorTarget.addDeploymentProcessor(DataSourcesExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_DATASOURCE_RESOURCE_INJECTION, new DefaultDataSourceResourceReferenceProcessor());
                processorTarget.addDeploymentProcessor(DataSourcesExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_DEFAULT_BINDINGS_DATASOURCE, new DefaultDataSourceBindingProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
