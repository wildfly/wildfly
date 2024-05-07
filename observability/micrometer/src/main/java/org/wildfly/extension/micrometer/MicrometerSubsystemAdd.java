/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;

class MicrometerSubsystemAdd extends AbstractBoottimeAddStepHandler {
    /*
    private final WildFlyCompositeRegistry wildFlyRegistry;

    MicrometerSubsystemAdd(WildFlyCompositeRegistry wildFlyRegistry) {
        this.wildFlyRegistry = wildFlyRegistry;
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {

        List<String> exposedSubsystems = MicrometerResourceDefinitionRegistrar.EXPOSED_SUBSYSTEMS.unwrap(context, model);
        boolean exposeAnySubsystem = exposedSubsystems.remove("*");

        MicrometerRegistryService.install(context, wildFlyRegistry);
        Supplier<MicrometerCollector> collectorSupplier = MicrometerCollectorService.install(context, wildFlyRegistry);

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(MicrometerSubsystemRegistrar.NAME, DEPENDENCIES, DEPENDENCIES_MICROMETER,
                        new MicrometerDependencyProcessor());
                processorTarget.addDeploymentProcessor(MicrometerSubsystemRegistrar.NAME, POST_MODULE, POST_MODULE_MICROMETER,
                        new MicrometerDeploymentProcessor(exposeAnySubsystem, exposedSubsystems, wildFlyRegistry));
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

    */
}
