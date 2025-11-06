/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.resource.definition;

import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.concurrent.WildFlyContextService;
import org.jboss.as.ee.concurrent.WildFlyManagedExecutorService;
import org.jboss.as.ee.concurrent.adapter.ManagedExecutorServiceAdapter;
import org.jboss.as.ee.concurrent.deployers.EEConcurrentDefaultBindingProcessor;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.concurrent.service.ManagedExecutorHungTasksPeriodicTerminationService;
import org.jboss.as.ee.concurrent.service.ManagedExecutorServiceService;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.ee.subsystem.ManagedExecutorServiceResourceDefinition;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.common.cpu.ProcessorInfo;
import org.wildfly.extension.requestcontroller.RequestController;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The {@link ResourceDefinitionInjectionSource} for {@link jakarta.enterprise.concurrent.ManagedExecutorDefinition}.
 *
 * @author emmartins
 */
public class ManagedExecutorDefinitionInjectionSource extends ResourceDefinitionInjectionSource {

    public static final String CONTEXT_PROP = "context";
    public static final String HUNG_TASK_THRESHOLD_PROP = "hungTaskThreshold";
    public static final String MAX_ASYNC_PROP = "maxAsync";

    private static final String REQUEST_CONTROLLER_CAPABILITY_NAME = "org.wildfly.request-controller";

    private String contextServiceRef;
    private long hungTaskThreshold;
    private int maxAsync = (ProcessorInfo.availableProcessors() * 2);
    private int hungTaskTerminationPeriod = 0;
    private boolean longRunningTasks = false;
    private long keepAliveTime = 60000;
    private TimeUnit keepAliveTimeUnit = TimeUnit.MILLISECONDS;
    private long threadLifeTime = 0L;
    private int queueLength = Integer.MAX_VALUE;
    private WildFlyManagedExecutorService.RejectPolicy rejectPolicy = WildFlyManagedExecutorService.RejectPolicy.ABORT;
    private int threadPriority = Thread.NORM_PRIORITY;

    public ManagedExecutorDefinitionInjectionSource(final String jndiName) {
        super(jndiName);
    }

    public void getResourceValue(final ResolutionContext context, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final String resourceName = uniqueName(context);
        final String resourceJndiName = "java:jboss/ee/concurrency/definition/managedExecutor/"+resourceName;
        final CapabilityServiceSupport capabilityServiceSupport = phaseContext.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);

        try {
            // install the resource service
            final ServiceName resourceServiceName = ManagedExecutorServiceResourceDefinition.CAPABILITY.getCapabilityServiceName(resourceName);
            final RequirementServiceBuilder<?> resourceServiceBuilder = phaseContext.getRequirementServiceTarget().addService();
            final Consumer<ManagedExecutorServiceAdapter> consumer = resourceServiceBuilder.provides(resourceServiceName);
            final Supplier<ManagedExecutorHungTasksPeriodicTerminationService> hungTasksPeriodicTerminationService = resourceServiceBuilder.requires(ConcurrentServiceNames.HUNG_TASK_PERIODIC_TERMINATION_SERVICE_NAME);
            final Supplier<ProcessStateNotifier> processStateNotifierSupplier = resourceServiceBuilder.requires(ProcessStateNotifier.SERVICE_DESCRIPTOR);
            Supplier<RequestController> requestControllerSupplier = null;
            if (capabilityServiceSupport.hasCapability(REQUEST_CONTROLLER_CAPABILITY_NAME)) {
                requestControllerSupplier = resourceServiceBuilder.requires(capabilityServiceSupport.getCapabilityServiceName(REQUEST_CONTROLLER_CAPABILITY_NAME));
            }
            final ManagedExecutorServiceService resourceService = new ManagedExecutorServiceService(consumer, null, null, processStateNotifierSupplier, requestControllerSupplier, resourceName, resourceJndiName, hungTaskThreshold, hungTaskTerminationPeriod, longRunningTasks, maxAsync, maxAsync, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueLength, rejectPolicy, threadPriority, hungTasksPeriodicTerminationService);
            resourceServiceBuilder.setInstance(resourceService);
            final Injector<ManagedReferenceFactory> contextServiceLookupInjector = new Injector<>() {
                @Override
                public void inject(ManagedReferenceFactory value) throws InjectionException {
                    resourceService.getContextServiceSupplier().set(() -> (WildFlyContextService) value.getReference().getInstance());
                }
                @Override
                public void uninject() {
                    resourceService.getContextServiceSupplier().set(() -> null);
                }
            };
            final String contextServiceRef;
            if (this.contextServiceRef == null || this.contextServiceRef.isEmpty() || this.contextServiceRef.equals(EEConcurrentDefaultBindingProcessor.COMP_DEFAULT_CONTEXT_SERVICE_JNDI_NAME)) {
                // default context service, use the real name of the resource since java:comp may not exist (e.g. ear)
                final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
                final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
                contextServiceRef = moduleDescription.getDefaultResourceJndiNames().getContextService();
            } else {
                contextServiceRef = this.contextServiceRef;
            }
            final ContextNames.BindInfo contextServiceBindInfo = ContextNames.bindInfoForEnvEntry(context.getApplicationName(), context.getModuleName(), context.getComponentName(), !context.isCompUsesModule(), contextServiceRef);
            contextServiceBindInfo.setupLookupInjection(resourceServiceBuilder, contextServiceLookupInjector, phaseContext.getDeploymentUnit(), false);
            resourceServiceBuilder.install();
            // use a dependency to the resource service installed to inject the resource
            serviceBuilder.addDependency(resourceServiceName, ManagedExecutorServiceAdapter.class, new Injector<>() {
                @Override
                public void inject(final ManagedExecutorServiceAdapter resource) throws InjectionException {
                    injector.inject(() -> new ManagedReference() {
                        @Override
                        public void release() {
                        }
                        @Override
                        public Object getInstance() {
                            return resource;
                        }
                    });
                }
                @Override
                public void uninject() {
                    injector.uninject();
                }
            });
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    public String getContextServiceRef() {
        return contextServiceRef;
    }

    public void setContextServiceRef(String contextServiceRef) {
        this.contextServiceRef = contextServiceRef;
    }

    public long getHungTaskThreshold() {
        return hungTaskThreshold;
    }

    public void setHungTaskThreshold(long hungTaskThreshold) {
        this.hungTaskThreshold = hungTaskThreshold;
    }

    public int getMaxAsync() {
        return maxAsync;
    }

    public void setMaxAsync(int maxAsync) {
        if (maxAsync > 0) {
            this.maxAsync = maxAsync;
        }
    }

    public int getHungTaskTerminationPeriod() {
        return hungTaskTerminationPeriod;
    }

    public void setHungTaskTerminationPeriod(int hungTaskTerminationPeriod) {
        this.hungTaskTerminationPeriod = hungTaskTerminationPeriod;
    }

    public boolean isLongRunningTasks() {
        return longRunningTasks;
    }

    public void setLongRunningTasks(boolean longRunningTasks) {
        this.longRunningTasks = longRunningTasks;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public TimeUnit getKeepAliveTimeUnit() {
        return keepAliveTimeUnit;
    }

    public void setKeepAliveTimeUnit(TimeUnit keepAliveTimeUnit) {
        this.keepAliveTimeUnit = keepAliveTimeUnit;
    }

    public long getThreadLifeTime() {
        return threadLifeTime;
    }

    public void setThreadLifeTime(long threadLifeTime) {
        this.threadLifeTime = threadLifeTime;
    }

    public int getQueueLength() {
        return queueLength;
    }

    public void setQueueLength(int queueLength) {
        this.queueLength = queueLength;
    }

    public WildFlyManagedExecutorService.RejectPolicy getRejectPolicy() {
        return rejectPolicy;
    }

    public void setRejectPolicy(WildFlyManagedExecutorService.RejectPolicy rejectPolicy) {
        this.rejectPolicy = rejectPolicy;
    }

    public int getThreadPriority() {
        return threadPriority;
    }

    public void setThreadPriority(int threadPriority) {
        this.threadPriority = threadPriority;
    }
}
