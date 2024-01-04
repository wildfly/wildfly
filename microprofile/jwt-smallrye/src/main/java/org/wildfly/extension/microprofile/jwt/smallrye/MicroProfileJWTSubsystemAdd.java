/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.jwt.smallrye;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.microprofile.jwt.smallrye._private.MicroProfileJWTLogger;

/**
 * Add handler for the MicroProfile JWT subsystem.
 *
 * <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class MicroProfileJWTSubsystemAdd extends AbstractBoottimeAddStepHandler {

    MicroProfileJWTSubsystemAdd() {
    }

    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model) {
        MicroProfileJWTLogger.ROOT_LOGGER.activatingSubsystem();

        if (context.isNormalServer()) {
            context.addStep(new AbstractDeploymentChainStep() {

                @Override
                protected void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(MicroProfileJWTExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_MICROPROFILE_JWT_DETECTION, new JwtActivationProcessor());
                    processorTarget.addDeploymentProcessor(MicroProfileJWTExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_MICROPROFILE_JWT, new JwtDependencyProcessor());
                }

            }, Stage.RUNTIME);
        }

    }
}
