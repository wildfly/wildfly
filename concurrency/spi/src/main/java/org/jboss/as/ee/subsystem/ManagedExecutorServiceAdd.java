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
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ee.concurrent.WildFlyManagedThreadFactory;
import org.jboss.as.ee.concurrent.WildFlyContextService;
import org.jboss.as.ee.concurrent.WildFlyManagedExecutorService;
import org.jboss.as.ee.concurrent.adapter.ManagedExecutorServiceAdapter;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.concurrent.service.ManagedExecutorHungTasksPeriodicTerminationService;
import org.jboss.as.ee.concurrent.service.ManagedExecutorServiceService;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.subsystem.ManagedExecutorServiceResourceDefinition.ExecutorQueueValidationStepHandler;
import org.jboss.dmr.ModelNode;
import org.wildfly.common.cpu.ProcessorInfo;
import org.wildfly.extension.requestcontroller.RequestController;

/**
 * @author Eduardo Martins
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ManagedExecutorServiceAdd extends AbstractAddStepHandler {

    private static final String REQUEST_CONTROLLER_CAPABILITY_NAME = "org.wildfly.request-controller";

    static final ManagedExecutorServiceAdd INSTANCE = new ManagedExecutorServiceAdd();

    @Override
    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {
        // Add a new step to validate the core-threads, max-threads and queue-length values
        context.addStep(ExecutorQueueValidationStepHandler.MODEL_VALIDATION_INSTANCE, OperationContext.Stage.MODEL);
        super.populateModel(context, operation, resource);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();

        final String jndiName = ManagedExecutorServiceResourceDefinition.JNDI_NAME_AD.resolveModelAttribute(context, model).asString();
        final long hungTaskTerminationPeriod = ManagedExecutorServiceResourceDefinition.HUNG_TASK_TERMINATION_PERIOD_AD.resolveModelAttribute(context, model).asLong();
        final long hungTaskThreshold = ManagedExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD_AD.resolveModelAttribute(context, model).asLong();
        final boolean longRunningTasks = ManagedExecutorServiceResourceDefinition.LONG_RUNNING_TASKS_AD.resolveModelAttribute(context, model).asBoolean();

        final int coreThreads;
        final ModelNode coreThreadsModel = ManagedExecutorServiceResourceDefinition.CORE_THREADS_AD.resolveModelAttribute(context, model);
        if (coreThreadsModel.isDefined()) {
            coreThreads = coreThreadsModel.asInt();
        } else {
            coreThreads = (ProcessorInfo.availableProcessors() * 2);
        }

        final int maxThreads;
        final ModelNode maxThreadsModel = ManagedExecutorServiceResourceDefinition.MAX_THREADS_AD.resolveModelAttribute(context, model);
        if (maxThreadsModel.isDefined()) {
            maxThreads = maxThreadsModel.asInt();
        } else {
            maxThreads = coreThreads;
        }

        // Note that this must be done in the runtime stage since the core-threads value may be calculated
        if (maxThreads < coreThreads) {
            throw EeLogger.ROOT_LOGGER.invalidMaxThreads(maxThreads, coreThreads);
        }

        final long keepAliveTime = ManagedExecutorServiceResourceDefinition.KEEPALIVE_TIME_AD.resolveModelAttribute(context, model).asLong();
        final TimeUnit keepAliveTimeUnit = TimeUnit.MILLISECONDS;
        final long threadLifeTime = 0L;

        final int queueLength;
        final ModelNode queueLengthModel = ManagedExecutorServiceResourceDefinition.QUEUE_LENGTH_AD.resolveModelAttribute(context, model);
        if (queueLengthModel.isDefined()) {
            queueLength = queueLengthModel.asInt();
        } else {
            queueLength = Integer.MAX_VALUE;
        }

        final WildFlyManagedExecutorService.RejectPolicy rejectPolicy = WildFlyManagedExecutorService.RejectPolicy.valueOf(ManagedExecutorServiceResourceDefinition.REJECT_POLICY_AD.resolveModelAttribute(context, model).asString());

        final Integer threadPriority;
        if(model.hasDefined(ManagedExecutorServiceResourceDefinition.THREAD_PRIORITY) || !model.hasDefined(ManagedExecutorServiceResourceDefinition.THREAD_FACTORY)) {
            // defined, or use default value in case deprecated thread-factory also not defined
            threadPriority = ManagedExecutorServiceResourceDefinition.THREAD_PRIORITY_AD.resolveModelAttribute(context, model).asInt();
        } else {
            // not defined and deprecated thread-factory is defined, use it instead
            threadPriority = null;
        }

        final CapabilityServiceBuilder<?> serviceBuilder = context.getCapabilityServiceTarget().addCapability(ManagedExecutorServiceResourceDefinition.CAPABILITY);
        final Consumer<ManagedExecutorServiceAdapter> consumer = serviceBuilder.provides(ManagedExecutorServiceResourceDefinition.CAPABILITY);
        final Supplier<ManagedExecutorHungTasksPeriodicTerminationService> hungTasksPeriodicTerminationService = serviceBuilder.requires(ConcurrentServiceNames.HUNG_TASK_PERIODIC_TERMINATION_SERVICE_NAME);
        String contextService = null;
        if (model.hasDefined(ManagedExecutorServiceResourceDefinition.CONTEXT_SERVICE)) {
            contextService = ManagedExecutorServiceResourceDefinition.CONTEXT_SERVICE_AD.resolveModelAttribute(context, model).asString();
        }
        final Supplier<WildFlyContextService> contextServiceSupplier = contextService != null ? serviceBuilder.requiresCapability(ContextServiceResourceDefinition.CAPABILITY.getName(), WildFlyContextService.class, contextService) : null;
        String threadFactory = null;
        if (model.hasDefined(ManagedExecutorServiceResourceDefinition.THREAD_FACTORY)) {
            threadFactory = ManagedExecutorServiceResourceDefinition.THREAD_FACTORY_AD.resolveModelAttribute(context, model).asString();
        }
        final Supplier<WildFlyManagedThreadFactory> threadFactorySupplier = threadFactory != null ? serviceBuilder.requiresCapability(ManagedThreadFactoryResourceDefinition.CAPABILITY.getName(), WildFlyManagedThreadFactory.class, threadFactory) : null;

        final Supplier<ProcessStateNotifier> processStateNotifierSupplier = serviceBuilder.requires(ProcessStateNotifier.SERVICE_DESCRIPTOR);
        Supplier<RequestController> requestControllerSupplier = null;
        if (context.hasOptionalCapability(REQUEST_CONTROLLER_CAPABILITY_NAME, ManagedExecutorServiceResourceDefinition.CAPABILITY.getDynamicName(context.getCurrentAddress()), null)) {
            requestControllerSupplier = serviceBuilder.requiresCapability(REQUEST_CONTROLLER_CAPABILITY_NAME, RequestController.class);
        }
        final ManagedExecutorServiceService service = new ManagedExecutorServiceService(consumer, contextServiceSupplier, threadFactorySupplier, processStateNotifierSupplier, requestControllerSupplier, name, jndiName, hungTaskThreshold, hungTaskTerminationPeriod, longRunningTasks, coreThreads, maxThreads, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueLength, rejectPolicy, threadPriority, hungTasksPeriodicTerminationService);
        serviceBuilder.setInstance(service);
        serviceBuilder.install();
    }

}
