/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.ee.concurrent.resource.definition;

import org.glassfish.enterprise.concurrent.AbstractManagedExecutorService;
import org.glassfish.enterprise.concurrent.ManagedScheduledExecutorServiceAdapter;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.concurrent.ContextServiceImpl;
import org.jboss.as.ee.concurrent.deployers.EEConcurrentDefaultBindingProcessor;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.concurrent.service.ManagedExecutorHungTasksPeriodicTerminationService;
import org.jboss.as.ee.concurrent.service.ManagedScheduledExecutorServiceService;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.ee.subsystem.ManagedScheduledExecutorServiceResourceDefinition;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.common.cpu.ProcessorInfo;
import org.wildfly.extension.requestcontroller.RequestController;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * The {@link ResourceDefinitionInjectionSource} for {@link jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition}.
 *
 * @author emmartins
 */
public class ManagedScheduledExecutorDefinitionInjectionSource extends ResourceDefinitionInjectionSource {

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
    private AbstractManagedExecutorService.RejectPolicy rejectPolicy = AbstractManagedExecutorService.RejectPolicy.ABORT;
    private int threadPriority = Thread.NORM_PRIORITY;

    public ManagedScheduledExecutorDefinitionInjectionSource(final String jndiName) {
        super(jndiName);
    }

    public void getResourceValue(final ResolutionContext context, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final String resourceName = uniqueName(context);
        final String resourceJndiName = "java:jboss/ee/concurrency/definition/managedScheduledExecutor/"+resourceName;
        final CapabilityServiceSupport capabilityServiceSupport = phaseContext.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);

        try {
            // install the resource service
            final ServiceName resourceServiceName = ManagedScheduledExecutorServiceResourceDefinition.CAPABILITY.getCapabilityServiceName(resourceName);
            final ServiceBuilder resourceServiceBuilder = phaseContext.getServiceTarget().addService(resourceServiceName);
            final Supplier<ManagedExecutorHungTasksPeriodicTerminationService> hungTasksPeriodicTerminationService = resourceServiceBuilder.requires(ConcurrentServiceNames.HUNG_TASK_PERIODIC_TERMINATION_SERVICE_NAME);
            final ManagedScheduledExecutorServiceService resourceService = new ManagedScheduledExecutorServiceService(resourceName, resourceJndiName, hungTaskThreshold, hungTaskTerminationPeriod, longRunningTasks, maxAsync, keepAliveTime, keepAliveTimeUnit, threadLifeTime, rejectPolicy, threadPriority, hungTasksPeriodicTerminationService);
            resourceServiceBuilder.setInstance(resourceService);
            final String contextServiceRef = this.contextServiceRef == null || this.contextServiceRef.isEmpty() ? EEConcurrentDefaultBindingProcessor.COMP_DEFAULT_CONTEXT_SERVICE_JNDI_NAME : this.contextServiceRef;
            final ContextNames.BindInfo contextServiceBindInfo = ContextNames.bindInfoForEnvEntry(context.getApplicationName(), context.getModuleName(), context.getComponentName(), !context.isCompUsesModule(), contextServiceRef);
            final Injector<ManagedReferenceFactory> contextServiceLookupInjector = new Injector<>() {
                @Override
                public void inject(ManagedReferenceFactory value) throws InjectionException {
                    resourceService.getContextServiceInjector().inject((ContextServiceImpl)value.getReference().getInstance());
                }
                @Override
                public void uninject() {
                    resourceService.getContextServiceInjector().uninject();
                }
            };
            contextServiceBindInfo.setupLookupInjection(resourceServiceBuilder, contextServiceLookupInjector, phaseContext.getDeploymentUnit(), false);
            if (capabilityServiceSupport.hasCapability(REQUEST_CONTROLLER_CAPABILITY_NAME)) {
                resourceServiceBuilder.addDependency(capabilityServiceSupport.getCapabilityServiceName(REQUEST_CONTROLLER_CAPABILITY_NAME), RequestController.class, resourceService.getRequestController());
            }
            resourceServiceBuilder.install();
            // use a dependency to the resource service installed to inject the resource
            serviceBuilder.addDependency(resourceServiceName, ManagedScheduledExecutorServiceAdapter.class, new Injector<>() {
                @Override
                public void inject(final ManagedScheduledExecutorServiceAdapter resource) throws InjectionException {
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

    public AbstractManagedExecutorService.RejectPolicy getRejectPolicy() {
        return rejectPolicy;
    }

    public void setRejectPolicy(AbstractManagedExecutorService.RejectPolicy rejectPolicy) {
        this.rejectPolicy = rejectPolicy;
    }

    public int getThreadPriority() {
        return threadPriority;
    }

    public void setThreadPriority(int threadPriority) {
        this.threadPriority = threadPriority;
    }
}
