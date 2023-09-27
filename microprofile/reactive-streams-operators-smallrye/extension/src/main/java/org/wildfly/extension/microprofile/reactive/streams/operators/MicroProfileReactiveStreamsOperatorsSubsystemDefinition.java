/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.streams.operators;

import static org.jboss.as.controller.OperationContext.Stage.RUNTIME;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES_MICROPROFILE_REACTIVE_STREAMS_OPERATORS;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.microprofile.reactive.streams.operators._private.MicroProfileReactiveStreamsOperatorsLogger;
import org.wildfly.extension.microprofile.reactive.streams.operators.deployment.ReactiveStreamsOperatorsDependencyProcessor;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MicroProfileReactiveStreamsOperatorsSubsystemDefinition extends PersistentResourceDefinition {

    private static final String REACTIVE_STREAMS_OPERATORS_CAPABILITY_NAME = "org.wildfly.microprofile.reactive-streams-operators";

    private static final RuntimeCapability<Void> REACTIVE_STREAMS_OPERATORS_CAPABILITY = RuntimeCapability.Builder
            .of(REACTIVE_STREAMS_OPERATORS_CAPABILITY_NAME)
            .addRequirements(MicroProfileReactiveStreamsOperatorsExtension.WELD_CAPABILITY_NAME)
            .build();

    public MicroProfileReactiveStreamsOperatorsSubsystemDefinition() {
        super(
                new SimpleResourceDefinition.Parameters(
                        MicroProfileReactiveStreamsOperatorsExtension.SUBSYSTEM_PATH,
                        MicroProfileReactiveStreamsOperatorsExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(new AddHandler())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(REACTIVE_STREAMS_OPERATORS_CAPABILITY)
        );
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        // Set up the dependencies needed by the deployments
        resourceRegistration.registerAdditionalRuntimePackages(
                RuntimePackageDependency.required("io.smallrye.reactive.mutiny.reactive-streams-operators"),
                RuntimePackageDependency.required("org.wildfly.reactive.mutiny.reactive-streams-operators.cdi-provider"),
                RuntimePackageDependency.required("org.wildfly.security.manager")
                );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    static class AddHandler extends AbstractBoottimeAddStepHandler {

        @Override
        protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performBoottime(context, operation, model);

            context.addStep(new AbstractDeploymentChainStep() {
                @Override
                public void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(MicroProfileReactiveStreamsOperatorsExtension.SUBSYSTEM_NAME, DEPENDENCIES, DEPENDENCIES_MICROPROFILE_REACTIVE_STREAMS_OPERATORS, new ReactiveStreamsOperatorsDependencyProcessor());
                }
            }, RUNTIME);

            MicroProfileReactiveStreamsOperatorsLogger.LOGGER.activatingSubsystem();
        }
    }
}
