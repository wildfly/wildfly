/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.messaging;

import static org.jboss.as.controller.OperationContext.Stage.RUNTIME;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES_MICROPROFILE_REACTIVE_MESSAGING;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.REACTIVE_STREAMS_OPERATORS_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.SUBSYSTEM_NAME;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.SUBSYSTEM_PATH;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.WELD_CAPABILITY_NAME;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.extension.microprofile.reactive.messaging._private.MicroProfileReactiveMessagingLogger;
import org.wildfly.extension.microprofile.reactive.messaging.deployment.ReactiveMessagingDependencyProcessor;
import org.wildfly.microprofile.reactive.messaging.common.security.ElytronSSLContextRegistry;
import org.wildfly.microprofile.reactive.messaging.config.ReactiveMessagingConfigSetter;
import org.wildfly.microprofile.reactive.messaging.config.TracingType;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MicroProfileReactiveMessagingSubsystemDefinition extends PersistentResourceDefinition {
    static final String REACTIVE_MESSAGING_CAPABILITY_NAME = "org.wildfly.microprofile.reactive-messaging";

    private static final RuntimeCapability<Void> REACTIVE_STREAMS_OPERATORS_CAPABILITY = RuntimeCapability.Builder
            .of(REACTIVE_MESSAGING_CAPABILITY_NAME)
            .addRequirements(WELD_CAPABILITY_NAME)
            .addRequirements(REACTIVE_STREAMS_OPERATORS_CAPABILITY_NAME)
            .build();

    public MicroProfileReactiveMessagingSubsystemDefinition() {
        super(
                new SimpleResourceDefinition.Parameters(
                        SUBSYSTEM_PATH,
                        MicroProfileReactiveMessagingExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(new AddHandler())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(REACTIVE_STREAMS_OPERATORS_CAPABILITY)

        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition.INSTANCE);
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(
                RuntimePackageDependency.required("io.reactivex.rxjava2.rxjava"),
                RuntimePackageDependency.required("io.smallrye.reactive.messaging"),
                RuntimePackageDependency.required("io.smallrye.reactive.messaging.connector"),
                RuntimePackageDependency.required("io.vertx.client"),
                RuntimePackageDependency.required("org.apache.commons.lang3"),
                RuntimePackageDependency.required("org.eclipse.microprofile.reactive-messaging.api"),
                RuntimePackageDependency.required("org.wildfly.reactive.messaging.config"),
                RuntimePackageDependency.required("org.slf4j"));

    }

    static class AddHandler extends AbstractBoottimeAddStepHandler {

        @Override
        protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performBoottime(context, operation, model);

            installElytronSSLContextRegistryServiceIfPresent(context);

            context.addStep(new AbstractDeploymentChainStep() {
                @Override
                public void execute(DeploymentProcessorTarget processorTarget) {
                    processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, DEPENDENCIES,
                            DEPENDENCIES_MICROPROFILE_REACTIVE_MESSAGING, new ReactiveMessagingDependencyProcessor());
                }


            }, RUNTIME);

            MicroProfileReactiveMessagingLogger.LOGGER.activatingSubsystem();
            Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS, true);

            TracingType amqpTracingType = TracingType.NEVER;
            TracingType kafkaTracingType = TracingType.NEVER;
            Resource openTelemetry = resource.getChild(MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition.PATH);

            if (openTelemetry != null) {
                ModelNode otelModel = openTelemetry.getModel();
                amqpTracingType = TracingType.valueOf(
                        MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition.AMQP
                                .resolveModelAttribute(context, otelModel).asString());
                kafkaTracingType = TracingType.valueOf(
                        MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition.KAFKA
                                .resolveModelAttribute(context, otelModel).asString());
            }
            ReactiveMessagingConfigSetter.setModelValues(amqpTracingType, kafkaTracingType);
        }

        private void installElytronSSLContextRegistryServiceIfPresent(OperationContext context) {
            ClassLoader cl = WildFlySecurityManager.getClassLoaderPrivileged(this.getClass());
            if (cl instanceof ModuleClassLoader) {
                ModuleLoader loader = ((ModuleClassLoader)cl).getModule().getModuleLoader();
                try {
                    loader.loadModule("org.wildfly.reactive.messaging.common");
                    ElytronSSLContextRegistry.setServiceRegistry(context.getServiceRegistry(false));
                } catch (ModuleLoadException e) {
                    // Ignore, it means the module is not available so we don't install the service
                }
            }
        }
    }
}
