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

package org.jboss.as.pojo;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

/**
 * Pojo subsystem add.
 * Define processors for POJO config handling.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Emanuel Muckenhuber
 */
class PojoSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final PojoSubsystemAdd INSTANCE = new PojoSubsystemAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) {

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(PojoExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_POJO_DEPLOYMENT, new KernelDeploymentParsingProcessor());
                processorTarget.addDeploymentProcessor(PojoExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.POST_MODULE_POJO, new KernelDeploymentModuleProcessor());
                processorTarget.addDeploymentProcessor(PojoExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_POJO_DEPLOYMENT, new ParsedKernelDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
