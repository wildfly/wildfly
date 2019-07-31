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

package org.wildfly.extension.microprofile.health;

import static org.jboss.as.controller.OperationContext.Stage.RUNTIME;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES_MICROPROFILE_HEALTH;
import static org.jboss.as.server.deployment.Phase.POST_MODULE;
import static org.jboss.as.server.deployment.Phase.POST_MODULE_MICROPROFILE_HEALTH;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.microprofile.health._private.MicroProfileHealthLogger;
import org.wildfly.extension.microprofile.health.deployment.DependencyProcessor;
import org.wildfly.extension.microprofile.health.deployment.DeploymentProcessor;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
class MicroProfileHealthSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static MicroProfileHealthSubsystemAdd INSTANCE = new MicroProfileHealthSubsystemAdd();

    private MicroProfileHealthSubsystemAdd() {
        super(MicroProfileHealthSubsystemDefinition.ATTRIBUTES);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performBoottime(context, operation, model);

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(MicroProfileHealthExtension.SUBSYSTEM_NAME, DEPENDENCIES, DEPENDENCIES_MICROPROFILE_HEALTH, new DependencyProcessor());
                processorTarget.addDeploymentProcessor(MicroProfileHealthExtension.SUBSYSTEM_NAME, POST_MODULE, POST_MODULE_MICROPROFILE_HEALTH, new DeploymentProcessor());
            }
        }, RUNTIME);

        final boolean securityEnabled = MicroProfileHealthSubsystemDefinition.SECURITY_ENABLED.resolveModelAttribute(context, model).asBoolean();
        final String emptyLivenessChecksStatus = MicroProfileHealthSubsystemDefinition.EMPTY_LIVENESS_CHECKS_STATUS.resolveModelAttribute(context, model).asString();
        final String emptyReadinessChecksStatus = MicroProfileHealthSubsystemDefinition.EMPTY_READINESS_CHECKS_STATUS.resolveModelAttribute(context, model).asString();
        HealthReporterService.install(context, emptyLivenessChecksStatus, emptyReadinessChecksStatus);
        HealthContextService.install(context, securityEnabled);

        MicroProfileHealthLogger.LOGGER.activatingSubsystem();
    }
}
