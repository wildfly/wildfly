/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.vertx;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.vertx.processors.DeploymentInjectionProcessor;
import org.wildfly.extension.vertx.processors.VerticleDeploymentMarkerProcessor;
import org.wildfly.extension.vertx.processors.VertxDependenciesProcessor;

/**
 * The root Vertx subsystem resource definition.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxSubsystemDefinition extends SimpleResourceDefinition {

    static final VertxSubsystemDefinition INSTANCE = new VertxSubsystemDefinition();

    VertxSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(VertxSubsystemExtension.SUBSYSTEM_PATH,
                VertxSubsystemRegistrar.RESOLVER)
                .setAddHandler(new VertxSubsystemAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
        );
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(VertxResourceDefinition.INSTANCE);
        resourceRegistration.registerSubModel(VertxOptionsResourceDefinition.INSTANCE);
        resourceRegistration.registerSubModel(AddressResolverResourceDefinition.INSTANCE);
    }

    static class VertxSubsystemAdd extends AbstractBoottimeAddStepHandler {
        @Override
        public void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            context.addStep(new AbstractDeploymentChainStep() {
                public void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(VertxSubsystemExtension.SUBSYSTEM_NAME,
                            VerticleDeploymentMarkerProcessor.PHASE, VerticleDeploymentMarkerProcessor.PRIORITY,
                            new VerticleDeploymentMarkerProcessor());
                    processorTarget.addDeploymentProcessor(VertxSubsystemExtension.SUBSYSTEM_NAME,
                            VertxDependenciesProcessor.PHASE, VertxDependenciesProcessor.PRIORITY,
                            new VertxDependenciesProcessor());
                    processorTarget.addDeploymentProcessor(VertxSubsystemExtension.SUBSYSTEM_NAME,
                      DeploymentInjectionProcessor.PHASE, DeploymentInjectionProcessor.PRIORITY,
                      new DeploymentInjectionProcessor());
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

}
