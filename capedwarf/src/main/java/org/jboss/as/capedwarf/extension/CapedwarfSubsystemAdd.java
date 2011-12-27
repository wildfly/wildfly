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

package org.jboss.as.capedwarf.extension;

import org.jboss.as.capedwarf.deployment.CapedwarfDependenciesProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfDeploymentProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfInitializationProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfJPAProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfWebCleanupProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfWebComponentsDeploymentProcessor;
import org.jboss.as.capedwarf.deployment.CapedwarfWeldProcessor;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.List;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class CapedwarfSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final CapedwarfSubsystemAdd INSTANCE = new CapedwarfSubsystemAdd();

    private CapedwarfSubsystemAdd() {
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }

    /** {@inheritDoc} */
    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
            ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        final String appengineAPI = operation.hasDefined("appengine-api") ? operation.get("appengine-api").asString() : null;

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT - 10, new CapedwarfInitializationProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT + 1, new CapedwarfWebCleanupProcessor()); // right after web.xml parsing
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEB_MERGE_METADATA + 1, new CapedwarfWebComponentsDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_WELD - 10, new CapedwarfWeldProcessor()); // before Weld
                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_JPA - 10, new CapedwarfJPAProcessor()); // before default JPA processor
                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_WAR_MODULE + 10, new CapedwarfDeploymentProcessor(appengineAPI));
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_APP_NAMING_CONTEXT + 10, new CapedwarfDependenciesProcessor()); // adjust order as needed
            }
        }, OperationContext.Stage.RUNTIME);

    }
}
