/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch.jberet.thread.pool;

import static org.wildfly.extension.batch.jberet.BatchResourceDescriptionResolver.BASE;
import static org.wildfly.extension.batch.jberet.BatchResourceDescriptionResolver.RESOURCE_NAME;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jberet.spi.JobExecutor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.threads.CommonAttributes;
import org.jboss.as.threads.ManagedJBossThreadPoolExecutorService;
import org.jboss.as.threads.PoolAttributeDefinitions;
import org.jboss.as.threads.ThreadFactoryResolver;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.threads.UnboundedQueueThreadPoolAdd;
import org.jboss.as.threads.UnboundedQueueThreadPoolMetricsHandler;
import org.jboss.as.threads.UnboundedQueueThreadPoolRemove;
import org.jboss.as.threads.UnboundedQueueThreadPoolWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.batch.jberet.BatchServiceNames;
import org.wildfly.extension.batch.jberet.BatchSubsystemExtension;
import org.wildfly.extension.batch.jberet._private.Capabilities;

/**
 * A resource definition for the batch thread pool.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class BatchThreadPoolResourceDefinition extends SimpleResourceDefinition {

    public static final String NAME = "thread-pool";
    static final PathElement PATH = PathElement.pathElement(NAME);

    private final boolean registerRuntimeOnly;

    public BatchThreadPoolResourceDefinition(final boolean registerRuntimeOnly) {
        super(new SimpleResourceDefinition.Parameters(PATH, BatchThreadPoolDescriptionResolver.INSTANCE)
                .setAddHandler(BatchThreadPoolAdd.INSTANCE)
                .setRemoveHandler(BatchThreadPoolRemove.INSTANCE)
                .addCapabilities(Capabilities.THREAD_POOL_CAPABILITY));
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(PoolAttributeDefinitions.NAME, ReadResourceNameOperationStepHandler.INSTANCE);
        new UnboundedQueueThreadPoolWriteAttributeHandler(BatchServiceNames.BASE_BATCH_THREAD_POOL_NAME).registerAttributes(resourceRegistration);
        if (registerRuntimeOnly) {
            new UnboundedQueueThreadPoolMetricsHandler(BatchServiceNames.BASE_BATCH_THREAD_POOL_NAME).registerAttributes(resourceRegistration);
        }
    }

    static class BatchThreadPoolAdd extends UnboundedQueueThreadPoolAdd {
        static final BatchThreadPoolAdd INSTANCE = new BatchThreadPoolAdd(BatchThreadFactoryResolver.INSTANCE, BatchServiceNames.BASE_BATCH_THREAD_POOL_NAME);
        private final ServiceName serviceNameBase;

        public BatchThreadPoolAdd(final ThreadFactoryResolver threadFactoryResolver, final ServiceName serviceNameBase) {
            super(threadFactoryResolver, serviceNameBase);
            this.serviceNameBase = serviceNameBase;
        }

        @Override
        protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);
            final String name = context.getCurrentAddressValue();
            final ServiceTarget target = context.getServiceTarget();
            final ServiceName serviceName = context.getCapabilityServiceName(Capabilities.THREAD_POOL_CAPABILITY.getName(), name, JobExecutor.class);
            final ServiceBuilder<?> serviceBuilder = target.addService(serviceName);
            final Consumer<JobExecutor> jobExecutorConsumer = serviceBuilder.provides(serviceName);
            final Supplier<ManagedJBossThreadPoolExecutorService> threadPoolSupplier = serviceBuilder.requires(serviceNameBase.append(name));
            final JobExecutorService service = new JobExecutorService(jobExecutorConsumer, threadPoolSupplier);
            serviceBuilder.setInstance(service);
            serviceBuilder.install();
        }
    }

    static class BatchThreadPoolRemove extends UnboundedQueueThreadPoolRemove {
        static final BatchThreadPoolRemove INSTANCE = new BatchThreadPoolRemove();

        public BatchThreadPoolRemove() {
            super(BatchThreadPoolAdd.INSTANCE);
        }

        @Override
        protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
            // First remove the JobExecutor service, then delegate
            context.removeService(context.getCapabilityServiceName(Capabilities.THREAD_POOL_CAPABILITY.getName(), context.getCurrentAddressValue(), null));
            super.performRuntime(context, operation, model);
        }
    }

    private static class BatchThreadFactoryResolver extends ThreadFactoryResolver.SimpleResolver {
        static final BatchThreadFactoryResolver INSTANCE = new BatchThreadFactoryResolver();

        private BatchThreadFactoryResolver() {
            super(ThreadsServices.FACTORY);
        }

        @Override
        protected String getThreadGroupName(String threadPoolName) {
            return "Batch Thread";
        }
    }

    private static class BatchThreadPoolDescriptionResolver extends StandardResourceDescriptionResolver {
        static final BatchThreadPoolDescriptionResolver INSTANCE = new BatchThreadPoolDescriptionResolver();

        private static final Set<String> COMMON_ATTRIBUTE_NAMES;

        static {
            // Common attributes as copied from the ThreadPoolResourceDescriptionResolver minus the attributes not used
            // for an UnboundedThreadPoolResourceDefinition
            COMMON_ATTRIBUTE_NAMES = new HashSet<>(Arrays.asList(
                    CommonAttributes.ACTIVE_COUNT,
                    CommonAttributes.COMPLETED_TASK_COUNT,
                    CommonAttributes.CURRENT_THREAD_COUNT,
                    CommonAttributes.KEEPALIVE_TIME,
                    CommonAttributes.LARGEST_THREAD_COUNT,
                    CommonAttributes.MAX_THREADS,
                    CommonAttributes.NAME,
                    CommonAttributes.QUEUE_SIZE,
                    CommonAttributes.TASK_COUNT,
                    CommonAttributes.THREAD_FACTORY));
        }

        private static final String COMMON_PREFIX = "threadpool.common";

        private BatchThreadPoolDescriptionResolver() {
            super(BASE + '.' + NAME, RESOURCE_NAME, BatchSubsystemExtension.class.getClassLoader(), true, false);
        }

        @Override
        public String getResourceAttributeDescription(final String attributeName, final Locale locale, final ResourceBundle bundle) {
            if (COMMON_ATTRIBUTE_NAMES.contains(attributeName)) {
                return bundle.getString(getKey(attributeName));
            }
            return super.getResourceAttributeDescription(attributeName, locale, bundle);
        }

        @Override
        public String getResourceAttributeValueTypeDescription(final String attributeName, final Locale locale, final ResourceBundle bundle, final String... suffixes) {
            if (COMMON_ATTRIBUTE_NAMES.contains(attributeName)) {
                return bundle.getString(getVariableBundleKey(COMMON_PREFIX, new String[]{attributeName}, suffixes));
            }
            return super.getResourceAttributeValueTypeDescription(attributeName, locale, bundle, suffixes);
        }

        @Override
        public String getOperationParameterDescription(final String operationName, final String paramName, final Locale locale, final ResourceBundle bundle) {
            if (ModelDescriptionConstants.ADD.equals(operationName) && COMMON_ATTRIBUTE_NAMES.contains(paramName)) {
                return bundle.getString(getKey(paramName));
            }
            return super.getOperationParameterDescription(operationName, paramName, locale, bundle);
        }

        @Override
        public String getOperationParameterValueTypeDescription(final String operationName, final String paramName, final Locale locale, final ResourceBundle bundle, final String... suffixes) {
            if (ModelDescriptionConstants.ADD.equals(operationName) && COMMON_ATTRIBUTE_NAMES.contains(paramName)) {
                return bundle.getString(getVariableBundleKey(COMMON_PREFIX, new String[]{paramName}, suffixes));
            }
            return super.getOperationParameterValueTypeDescription(operationName, paramName, locale, bundle, suffixes);
        }

        private String getKey(final String... args) {
            return getVariableBundleKey(COMMON_PREFIX, args);
        }
    }
}
