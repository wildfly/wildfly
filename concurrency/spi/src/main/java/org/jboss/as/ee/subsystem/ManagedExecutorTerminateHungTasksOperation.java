/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ee.concurrent.WildFlyManagedExecutorService;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Operation that manually terminates a managed executor's hung tasks, through its capability service.
 *
 * @author emartins
 */
public class ManagedExecutorTerminateHungTasksOperation<T> {

    public static final String NAME = "terminate-hung-tasks";

    private final RuntimeCapability capability;
    private final SimpleOperationDefinition operationDefinition;
    private final ExecutorProvider<T> executorProvider;

    /**
     *
     * @param capability
     * @param resolver
     * @param executorProvider
     */
    ManagedExecutorTerminateHungTasksOperation(final RuntimeCapability capability, ResourceDescriptionResolver resolver, ExecutorProvider<T> executorProvider) {
        this.capability = capability;
        this.operationDefinition = new SimpleOperationDefinitionBuilder(NAME, resolver)
                .setRuntimeOnly()
                .build();
        this.executorProvider = executorProvider;
    }

    /**
     * Registers this operation with the specified resource.
     * @param resourceRegistration
     */
    void registerOperation(ManagementResourceRegistration resourceRegistration) {
        if (resourceRegistration.getProcessType().isServer()) {
            resourceRegistration.registerOperationHandler(operationDefinition, new AbstractRuntimeOnlyHandler() {
                @Override
                protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
                    if (context.getRunningMode() == RunningMode.NORMAL) {
                        ServiceName serviceName = capability.getCapabilityServiceName(context.getCurrentAddress());
                        ServiceController<?> controller = context.getServiceRegistry(false).getService(serviceName);
                        if (controller == null) {
                            throw EeLogger.ROOT_LOGGER.executorServiceNotFound(serviceName);
                        }
                        final T service = (T) controller.getService();
                        WildFlyManagedExecutorService executor = executorProvider.getExecutor(service);
                        executor.terminateHungTasks();
                    }
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }
            });
        }
    }

    /**
     * Component which retrieves executors with hung threads, from services.
     * @param <T>
     */
    interface ExecutorProvider<T> {
        /**
         *
         * @param service
         * @return the executor with the specified service
         */
        WildFlyManagedExecutorService getExecutor(T service);
    }
}
