/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer;

import static org.jboss.as.controller.OperationContext.Stage.RUNTIME;
import static org.jboss.as.controller.OperationContext.Stage.VERIFY;
import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES_MICROMETER;
import static org.jboss.as.server.deployment.Phase.POST_MODULE;
import static org.jboss.as.server.deployment.Phase.POST_MODULE_MICROMETER;
import static org.wildfly.extension.micrometer.MicrometerExtension.SUBSYSTEM_NAME;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;

class MicrometerSubsystemAdd extends AbstractBoottimeAddStepHandler {
    MicrometerSubsystemAdd() {
        super(MicrometerSubsystemDefinition.ATTRIBUTES);
    }

    public static final MicrometerSubsystemAdd INSTANCE = new MicrometerSubsystemAdd();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {

        List<String> exposedSubsystems = MicrometerSubsystemDefinition.EXPOSED_SUBSYSTEMS.unwrap(context, model);
        boolean exposeAnySubsystem = exposedSubsystems.remove("*");
        String endpoint = MicrometerSubsystemDefinition.ENDPOINT.resolveModelAttribute(context, model)
                .asStringOrNull();
        Long step = MicrometerSubsystemDefinition.STEP.resolveModelAttribute(context, model)
                .asLong();

        WildFlyMicrometerConfig config = new WildFlyMicrometerConfig(endpoint, step);
        Supplier<WildFlyRegistry> registrySupplier = MicrometerRegistryService.install(context, config);
        Supplier<MicrometerCollector> collectorSupplier = MicrometerCollectorService.install(context);

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, DEPENDENCIES, DEPENDENCIES_MICROMETER,
                        new MicrometerDependencyProcessor());
                processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, POST_MODULE, POST_MODULE_MICROMETER,
                        new MicrometerDeploymentProcessor(exposeAnySubsystem, exposedSubsystems, registrySupplier));
            }
        }, RUNTIME);

        context.addStep((operationContext, modelNode) -> {
            MicrometerCollector micrometerCollector = collectorSupplier.get();

            if (micrometerCollector == null) {
                throw new IllegalStateException();
            }

            ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
            Resource rootResource = context.readResourceFromRoot(EMPTY_ADDRESS);

            micrometerCollector.collectResourceMetrics(rootResource, rootResourceRegistration,
                    Function.identity(), exposeAnySubsystem, exposedSubsystems);
        }, VERIFY);


        MicrometerExtensionLogger.MICROMETER_LOGGER.activatingSubsystem();
    }
}
