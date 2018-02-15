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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.jberet.spi.JobExecutor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
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
import org.wildfly.extension.batch.jberet.BatchResourceDescriptionResolver;
import org.wildfly.extension.batch.jberet.BatchServiceNames;
import org.wildfly.extension.batch.jberet._private.Capabilities;

/**
 * A resource definition for the batch thread pool.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchThreadPoolResourceDefinition extends SimpleResourceDefinition {

    public static final String NAME = "thread-pool";
    static final PathElement PATH = PathElement.pathElement(NAME);

    private final boolean registerRuntimeOnly;

    public BatchThreadPoolResourceDefinition(final boolean registerRuntimeOnly) {
        super(PATH, BatchThreadPoolDescriptionResolver.INSTANCE,
                BatchThreadPoolAdd.INSTANCE, BatchThreadPoolRemove.INSTANCE);
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

    @Override
    public void registerCapabilities(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerCapability(Capabilities.THREAD_POOL_CAPABILITY);
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
            final JobExecutorService service = new JobExecutorService();
            final ServiceBuilder<?> serviceBuilder = target.addService(context.getCapabilityServiceName(Capabilities.THREAD_POOL_CAPABILITY.getName(), name, JobExecutor.class),
                    service);
            serviceBuilder.addDependency(serviceNameBase.append(name), ManagedJBossThreadPoolExecutorService.class, service.getThreadPoolInjector());
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

    private static class BatchThreadPoolDescriptionResolver implements ResourceDescriptionResolver {
        static final BatchThreadPoolDescriptionResolver INSTANCE = new BatchThreadPoolDescriptionResolver();

        private static final Set<String> COMMON_ATTRIBUTE_NAMES;

        static {
            // Common attributes as copied from the ThreadPoolResourceDescriptionResolver minus the attributes not used
            // for an UnboundedThreadPoolResourceDefinition
            Set<String> set = new HashSet<>();
            for (String s : Arrays.asList(
                PoolAttributeDefinitions.ACTIVE_COUNT.getName(),
                PoolAttributeDefinitions.COMPLETED_TASK_COUNT.getName(),
                PoolAttributeDefinitions.CURRENT_THREAD_COUNT.getName(),
                CommonAttributes.KEEPALIVE_TIME,
                PoolAttributeDefinitions.LARGEST_THREAD_COUNT.getName(),
                PoolAttributeDefinitions.MAX_THREADS.getName(),
                PoolAttributeDefinitions.NAME.getName(),
                PoolAttributeDefinitions.QUEUE_SIZE.getName(),
                PoolAttributeDefinitions.TASK_COUNT.getName(),
                PoolAttributeDefinitions.THREAD_FACTORY.getName()
            )) {
                set.add(s);
            }
            COMMON_ATTRIBUTE_NAMES = set;
        }

        private static final String COMMON_PREFIX = "threadpool.common";
        private final StandardResourceDescriptionResolver delegate;

        private BatchThreadPoolDescriptionResolver() {
            this.delegate = BatchResourceDescriptionResolver.getResourceDescriptionResolver(NAME);
        }

        @Override
        public ResourceBundle getResourceBundle(final Locale locale) {
            return delegate.getResourceBundle(locale);
        }

        @Override
        public String getResourceDescription(final Locale locale, final ResourceBundle bundle) {
            return delegate.getResourceDescription(locale, bundle);
        }

        @Override
        public String getResourceAttributeDescription(final String attributeName, final Locale locale, final ResourceBundle bundle) {
            if (COMMON_ATTRIBUTE_NAMES.contains(attributeName)) {
                return bundle.getString(getKey(attributeName));
            }
            return delegate.getResourceAttributeDescription(attributeName, locale, bundle);
        }

        @Override
        public String getResourceAttributeValueTypeDescription(final String attributeName, final Locale locale, final ResourceBundle bundle, final String... suffixes) {
            if (COMMON_ATTRIBUTE_NAMES.contains(attributeName)) {
                return bundle.getString(getVariableBundleKey(new String[]{attributeName}, suffixes));
            }
            return delegate.getResourceAttributeValueTypeDescription(attributeName, locale, bundle, suffixes);
        }

        @Override
        public String getOperationDescription(final String operationName, final Locale locale, final ResourceBundle bundle) {
            return delegate.getOperationDescription(operationName, locale, bundle);
        }

        @Override
        public String getOperationParameterDescription(final String operationName, final String paramName, final Locale locale, final ResourceBundle bundle) {
            if (ModelDescriptionConstants.ADD.equals(operationName) && COMMON_ATTRIBUTE_NAMES.contains(paramName)) {
                return bundle.getString(getKey(paramName));
            }
            return delegate.getOperationParameterDescription(operationName, paramName, locale, bundle);
        }

        @Override
        public String getOperationParameterValueTypeDescription(final String operationName, final String paramName, final Locale locale, final ResourceBundle bundle, final String... suffixes) {
            if (ModelDescriptionConstants.ADD.equals(operationName) && COMMON_ATTRIBUTE_NAMES.contains(paramName)) {
                return bundle.getString(getVariableBundleKey(new String[]{paramName}, suffixes));
            }
            return delegate.getOperationParameterValueTypeDescription(operationName, paramName, locale, bundle, suffixes);
        }

        @Override
        public String getOperationReplyDescription(final String operationName, final Locale locale, final ResourceBundle bundle) {
            return delegate.getOperationReplyDescription(operationName, locale, bundle);
        }

        @Override
        public String getOperationReplyValueTypeDescription(final String operationName, final Locale locale, final ResourceBundle bundle, final String... suffixes) {
            return delegate.getOperationReplyValueTypeDescription(operationName, locale, bundle, suffixes);
        }

        @Override
        public String getNotificationDescription(final String notificationType, final Locale locale, final ResourceBundle bundle) {
            return delegate.getNotificationDescription(notificationType, locale, bundle);
        }

        @Override
        public String getChildTypeDescription(final String childType, final Locale locale, final ResourceBundle bundle) {
            return delegate.getChildTypeDescription(childType, locale, bundle);
        }

        @Override
        public String getResourceDeprecatedDescription(final Locale locale, final ResourceBundle bundle) {
            return delegate.getResourceDeprecatedDescription(locale, bundle);
        }

        @Override
        public String getResourceAttributeDeprecatedDescription(final String attributeName, final Locale locale, final ResourceBundle bundle) {
            return delegate.getResourceAttributeDeprecatedDescription(attributeName, locale, bundle);
        }

        @Override
        public String getOperationDeprecatedDescription(final String operationName, final Locale locale, final ResourceBundle bundle) {
            return delegate.getOperationDeprecatedDescription(operationName, locale, bundle);
        }

        @Override
        public String getOperationParameterDeprecatedDescription(final String operationName, final String paramName, final Locale locale, final ResourceBundle bundle) {
            return delegate.getOperationParameterDeprecatedDescription(operationName, paramName, locale, bundle);
        }


        private String getKey(final String... args) {
            return getVariableBundleKey(args);
        }

        private String getVariableBundleKey(final String[] fixed, final String... variable) {
            StringBuilder sb = new StringBuilder(COMMON_PREFIX);
            for (String arg : fixed) {
                sb.append('.');
                sb.append(arg);
            }
            if (variable != null) {
                for (String arg : variable) {
                    sb.append('.');
                    sb.append(arg);
                }
            }
            return sb.toString();
        }
    }
}
