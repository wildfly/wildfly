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
