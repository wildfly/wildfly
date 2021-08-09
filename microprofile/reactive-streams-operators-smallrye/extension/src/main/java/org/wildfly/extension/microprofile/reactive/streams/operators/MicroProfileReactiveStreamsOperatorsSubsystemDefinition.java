/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
                        MicroProfileReactiveStreamsOperatorsExtension.getResourceDescriptionResolver(MicroProfileReactiveStreamsOperatorsExtension.SUBSYSTEM_NAME))
                .setAddHandler(AddHandler.INSTANCE)
                .setRemoveHandler(new ReloadRequiredRemoveStepHandler())
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

        static AddHandler INSTANCE = new AddHandler();

        private AddHandler() {
            super(Collections.emptyList());
        }

        @Override
        protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performBoottime(context, operation, model);

            context.addStep(new AbstractDeploymentChainStep() {
                public void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(MicroProfileReactiveStreamsOperatorsExtension.SUBSYSTEM_NAME, DEPENDENCIES, DEPENDENCIES_MICROPROFILE_REACTIVE_STREAMS_OPERATORS, new ReactiveStreamsOperatorsDependencyProcessor());
                }
            }, RUNTIME);

            MicroProfileReactiveStreamsOperatorsLogger.LOGGER.activatingSubsystem();
        }
    }
}
