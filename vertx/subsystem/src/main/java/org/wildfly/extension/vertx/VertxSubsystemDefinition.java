/*
 * Copyright (C) 2020 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    VertxSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(VertxSubsystemExtension.SUBSYSTEM_PATH,
                VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME))
                .setAddHandler(new VertxSubsystemAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
        );
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(VertxResourceDefinition.INSTANCE);
        resourceRegistration.registerSubModel(VertxOptionFileResourceDefinition.INSTANCE);
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
