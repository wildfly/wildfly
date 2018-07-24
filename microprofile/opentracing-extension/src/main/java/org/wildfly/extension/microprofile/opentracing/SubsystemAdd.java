/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.opentracing;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

class SubsystemAdd extends AbstractBoottimeAddStepHandler {
    static final SubsystemAdd INSTANCE = new SubsystemAdd();

    private SubsystemAdd() {
        super();
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) {
        TracingExtensionLogger.ROOT_LOGGER.activatingSubsystem();
        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(
                        SubsystemExtension.SUBSYSTEM_NAME,
                        Phase.DEPENDENCIES,
                        Phase.DEPENDENCIES_MODULE,
                        new TracingDependencyProcessor()
                );

                processorTarget.addDeploymentProcessor(SubsystemExtension.SUBSYSTEM_NAME,
                        Phase.POST_MODULE,
                        Phase.POST_MODULE_WELD_PORTABLE_EXTENSIONS + 11,
                        new TracingDeploymentProcessor()
                );
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
