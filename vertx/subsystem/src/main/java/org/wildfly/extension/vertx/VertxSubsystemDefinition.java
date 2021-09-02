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
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.vertx.deployment.VertxDependenciesProcessor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The root Vertx subsystem resource definition.
 *
 * It has multiple VertxDefinitions to represent a Vert.x instance for each.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxSubsystemDefinition extends PersistentResourceDefinition {

    private static final SimpleOperationDefinition LIST_VERTX_OPERATION = new SimpleOperationDefinitionBuilder("list-vertx",
            VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME))
            .setRuntimeOnly()
            .setReplyType(ModelType.LIST)
            .setReplyValueType(ModelType.OBJECT)
            .build();

    VertxSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(VertxSubsystemExtension.SUBSYSTEM_PATH,
                VertxSubsystemExtension.getResourceDescriptionResolver(VertxSubsystemExtension.SUBSYSTEM_NAME))
                .setAddHandler(new VertxSubsystemAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
        );
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return Collections.singletonList(VertxResourceDefinition.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerOperationHandler(LIST_VERTX_OPERATION, new ListAllVertxOperation());
        super.registerOperations(resourceRegistration);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    static class VertxSubsystemAdd extends AbstractBoottimeAddStepHandler {
        @Override
        public void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            //Add deployment processors here
            //Remove this if you don't need to hook into the deployers, or you can add as many as you like
            //see SubDeploymentProcessor for explanation of the phases
            context.addStep(new AbstractDeploymentChainStep() {
                public void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(VertxSubsystemExtension.SUBSYSTEM_NAME,
                            VertxDependenciesProcessor.PHASE, VertxDependenciesProcessor.PRIORITY,
                            new VertxDependenciesProcessor());
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

    static class ListAllVertxOperation implements OperationStepHandler {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode vertx = new ModelNode();
            for (VertxProxy v: VertxRegistry.INSTANCE.listVertx()) {
                ModelNode vp = new ModelNode();
                vp.get(VertxConstants.NAME).set(v.getName());
                vp.get(VertxConstants.JNDI_NAME).set(v.getJndiName());
                vertx.add(vp);
            }
            context.getResult().set(vertx);
        }
    }
}
