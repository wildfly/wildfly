/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.ee.concurrent.WildFlyManagedThreadFactory;
import org.jboss.as.ee.concurrent.WildFlyContextService;
import org.jboss.as.ee.concurrent.WildFlyManagedExecutorService;
import org.jboss.as.ee.concurrent.adapter.ManagedScheduledExecutorServiceAdapter;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.concurrent.service.ManagedExecutorHungTasksPeriodicTerminationService;
import org.jboss.as.ee.concurrent.service.ManagedScheduledExecutorServiceService;
import org.jboss.dmr.ModelNode;
import org.wildfly.common.cpu.ProcessorInfo;
import org.wildfly.extension.requestcontroller.RequestController;

/**
 * @author Eduardo Martins
 */
public class ManagedScheduledExecutorServiceAdd extends AbstractAddStepHandler {

    private static final String REQUEST_CONTROLLER_CAPABILITY_NAME = "org.wildfly.request-controller";

    static final ManagedScheduledExecutorServiceAdd INSTANCE = new ManagedScheduledExecutorServiceAdd();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final String name = context.getCurrentAddressValue();

        final String jndiName = ManagedExecutorServiceResourceDefinition.JNDI_NAME_AD.resolveModelAttribute(context, model).asString();
        final long hungTaskTerminationPeriod = ManagedScheduledExecutorServiceResourceDefinition.HUNG_TASK_TERMINATION_PERIOD_AD.resolveModelAttribute(context, model).asLong();
        final long hungTaskThreshold = ManagedScheduledExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD_AD.resolveModelAttribute(context, model).asLong();
        final boolean longRunningTasks = ManagedScheduledExecutorServiceResourceDefinition.LONG_RUNNING_TASKS_AD.resolveModelAttribute(context, model).asBoolean();

        final int coreThreads;
        final ModelNode coreThreadsModel = ManagedScheduledExecutorServiceResourceDefinition.CORE_THREADS_AD.resolveModelAttribute(context, model);
        //value 0 means the same as undefined
        if (coreThreadsModel.isDefined() && coreThreadsModel.asInt() != 0) {
            coreThreads = coreThreadsModel.asInt();
        } else {
            coreThreads = (ProcessorInfo.availableProcessors() * 2);
        }

        final long keepAliveTime = ManagedScheduledExecutorServiceResourceDefinition.KEEPALIVE_TIME_AD.resolveModelAttribute(context, model).asLong();
        final TimeUnit keepAliveTimeUnit = TimeUnit.MILLISECONDS;
        final long threadLifeTime = 0L;
        final WildFlyManagedExecutorService.RejectPolicy rejectPolicy = WildFlyManagedExecutorService.RejectPolicy.valueOf(ManagedScheduledExecutorServiceResourceDefinition.REJECT_POLICY_AD.resolveModelAttribute(context, model).asString());

        final Integer threadPriority;
        if(model.hasDefined(ManagedScheduledExecutorServiceResourceDefinition.THREAD_PRIORITY) || !model.hasDefined(ManagedScheduledExecutorServiceResourceDefinition.THREAD_FACTORY)) {
            // defined, or use default value in case deprecated thread-factory also not defined
            threadPriority = ManagedScheduledExecutorServiceResourceDefinition.THREAD_PRIORITY_AD.resolveModelAttribute(context, model).asInt();
        } else {
            // not defined and deprecated thread-factory is defined, use it instead
            threadPriority = null;
        }

        final CapabilityServiceBuilder<?> serviceBuilder = context.getCapabilityServiceTarget().addCapability(ManagedScheduledExecutorServiceResourceDefinition.CAPABILITY);
        final Consumer<ManagedScheduledExecutorServiceAdapter> consumer = serviceBuilder.provides(ManagedScheduledExecutorServiceResourceDefinition.CAPABILITY);
        final Supplier<ManagedExecutorHungTasksPeriodicTerminationService> hungTasksPeriodicTerminationService = serviceBuilder.requires(ConcurrentServiceNames.HUNG_TASK_PERIODIC_TERMINATION_SERVICE_NAME);
        String contextService = null;
        if (model.hasDefined(ManagedScheduledExecutorServiceResourceDefinition.CONTEXT_SERVICE)) {
            contextService = ManagedScheduledExecutorServiceResourceDefinition.CONTEXT_SERVICE_AD.resolveModelAttribute(context, model).asString();
        }
        final Supplier<WildFlyContextService> contextServiceSupplier = contextService != null ? serviceBuilder.requiresCapability(ContextServiceResourceDefinition.CAPABILITY.getName(), WildFlyContextService.class, contextService) : null;
        String threadFactory = null;
        if (model.hasDefined(ManagedScheduledExecutorServiceResourceDefinition.THREAD_FACTORY)) {
            threadFactory = ManagedScheduledExecutorServiceResourceDefinition.THREAD_FACTORY_AD.resolveModelAttribute(context, model).asString();
        }
        final Supplier<WildFlyManagedThreadFactory> managedThreadFactorySupplier = threadFactory != null ? serviceBuilder.requiresCapability(ManagedThreadFactoryResourceDefinition.CAPABILITY.getName(), WildFlyManagedThreadFactory.class, threadFactory) : null;

        final Supplier<ProcessStateNotifier> processStateNotifierSupplier = serviceBuilder.requires(ProcessStateNotifier.SERVICE_DESCRIPTOR);
        Supplier<RequestController> requestControllerSupplier = null;
        if (context.hasOptionalCapability(REQUEST_CONTROLLER_CAPABILITY_NAME, ManagedScheduledExecutorServiceResourceDefinition.CAPABILITY.getDynamicName(context.getCurrentAddress()), null)) {
            requestControllerSupplier = serviceBuilder.requiresCapability(REQUEST_CONTROLLER_CAPABILITY_NAME, RequestController.class);
        }
        final ManagedScheduledExecutorServiceService service = new ManagedScheduledExecutorServiceService(consumer, contextServiceSupplier, managedThreadFactorySupplier, processStateNotifierSupplier, requestControllerSupplier, name, jndiName, hungTaskThreshold, hungTaskTerminationPeriod, longRunningTasks, coreThreads, keepAliveTime, keepAliveTimeUnit, threadLifeTime, rejectPolicy, threadPriority, hungTasksPeriodicTerminationService);
        serviceBuilder.setInstance(service);
        serviceBuilder.install();
    }

}
