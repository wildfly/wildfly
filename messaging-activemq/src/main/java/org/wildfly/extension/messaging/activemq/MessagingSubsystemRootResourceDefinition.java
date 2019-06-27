/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.COUNTER_METRIC;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.GAUGE_METRIC;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the messaging subsystem root resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingSubsystemRootResourceDefinition extends PersistentResourceDefinition {
    private static final String GLOBAL_CLIENT_PREFIX = "global-client-thread-pool-";
    private static final String GLOBAL_CLIENT_SCHEDULED_PREFIX = "global-client-scheduled-thread-pool-";

    public static final RuntimeCapability<Void> CONFIGURATION_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.messaging.activemq.external.configuration", false)
            .setServiceType(ExternalBrokerConfigurationService.class)
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_THREAD_POOL_MAX_SIZE = create(GLOBAL_CLIENT_PREFIX + "max-size", INT)
            .setAttributeGroup("global-client")
            .setXmlName("thread-pool-max-size")
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_THREAD_POOL_ACTIVE_COUNT = create(GLOBAL_CLIENT_PREFIX + org.jboss.as.threads.CommonAttributes.ACTIVE_COUNT, INT)
            .setAttributeGroup("global-client")
            .setUndefinedMetricValue(ModelNode.ZERO)
            .addFlag(GAUGE_METRIC)
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_THREAD_POOL_COMPLETED_TASK_COUNT = create(GLOBAL_CLIENT_PREFIX + org.jboss.as.threads.CommonAttributes.COMPLETED_TASK_COUNT, INT)
            .setAttributeGroup("global-client")
            .setUndefinedMetricValue(ModelNode.ZERO)
            .addFlag(COUNTER_METRIC)
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_THREAD_POOL_CURRENT_THREAD_COUNT = create(GLOBAL_CLIENT_PREFIX + org.jboss.as.threads.CommonAttributes.CURRENT_THREAD_COUNT, INT)
            .setAttributeGroup("global-client")
            .setUndefinedMetricValue(ModelNode.ZERO)
            .addFlag(GAUGE_METRIC)
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_THREAD_POOL_LARGEST_THREAD_COUNT = create(GLOBAL_CLIENT_PREFIX + org.jboss.as.threads.CommonAttributes.LARGEST_THREAD_COUNT, INT)
            .setAttributeGroup("global-client")
            .setUndefinedMetricValue(ModelNode.ZERO)
            .addFlag(COUNTER_METRIC)
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_THREAD_POOL_TASK_COUNT = create(GLOBAL_CLIENT_PREFIX + org.jboss.as.threads.CommonAttributes.TASK_COUNT, INT)
            .setAttributeGroup("global-client")
            .setUndefinedMetricValue(ModelNode.ZERO)
            .addFlag(COUNTER_METRIC)
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_THREAD_POOL_KEEPALIVE_TIME = create(GLOBAL_CLIENT_PREFIX + org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME, LONG)
            .setAttributeGroup("global-client")
            .setUndefinedMetricValue(ModelNode.ZERO)
            .setMeasurementUnit(MeasurementUnit.NANOSECONDS)
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_MAX_SIZE = create(GLOBAL_CLIENT_SCHEDULED_PREFIX + "max-size", INT)
            .setAttributeGroup("global-client")
            .setXmlName("scheduled-thread-pool-max-size")
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_ACTIVE_COUNT = create(GLOBAL_CLIENT_SCHEDULED_PREFIX + org.jboss.as.threads.CommonAttributes.ACTIVE_COUNT, INT)
            .setAttributeGroup("global-client")
            .setUndefinedMetricValue(ModelNode.ZERO)
            .addFlag(GAUGE_METRIC)
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_COMPLETED_TASK_COUNT = create(GLOBAL_CLIENT_SCHEDULED_PREFIX + org.jboss.as.threads.CommonAttributes.COMPLETED_TASK_COUNT, INT)
            .setAttributeGroup("global-client")
            .setUndefinedMetricValue(ModelNode.ZERO)
            .addFlag(COUNTER_METRIC)
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_CURRENT_THREAD_COUNT = create(GLOBAL_CLIENT_SCHEDULED_PREFIX + org.jboss.as.threads.CommonAttributes.CURRENT_THREAD_COUNT, INT)
            .setAttributeGroup("global-client")
            .setUndefinedMetricValue(ModelNode.ZERO)
            .addFlag(GAUGE_METRIC)
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_LARGEST_THREAD_COUNT = create(GLOBAL_CLIENT_SCHEDULED_PREFIX + org.jboss.as.threads.CommonAttributes.LARGEST_THREAD_COUNT, INT)
            .setAttributeGroup("global-client")
            .setUndefinedMetricValue(ModelNode.ZERO)
            .addFlag(COUNTER_METRIC)
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_TASK_COUNT = create(GLOBAL_CLIENT_SCHEDULED_PREFIX + org.jboss.as.threads.CommonAttributes.TASK_COUNT, INT)
            .setAttributeGroup("global-client")
            .setUndefinedMetricValue(ModelNode.ZERO)
            .addFlag(COUNTER_METRIC)
            .build();

    public static final SimpleAttributeDefinition GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_KEEPALIVE_TIME = create(GLOBAL_CLIENT_SCHEDULED_PREFIX + org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME, LONG)
            .setAttributeGroup("global-client")
            .setUndefinedMetricValue(ModelNode.ZERO)
            .setMeasurementUnit(MeasurementUnit.NANOSECONDS)
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {
        GLOBAL_CLIENT_THREAD_POOL_MAX_SIZE,
        GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_MAX_SIZE
    };
    public static final AttributeDefinition[] METRICS = {
        GLOBAL_CLIENT_THREAD_POOL_ACTIVE_COUNT, GLOBAL_CLIENT_THREAD_POOL_COMPLETED_TASK_COUNT, GLOBAL_CLIENT_THREAD_POOL_CURRENT_THREAD_COUNT,
        GLOBAL_CLIENT_THREAD_POOL_LARGEST_THREAD_COUNT, GLOBAL_CLIENT_THREAD_POOL_TASK_COUNT, GLOBAL_CLIENT_THREAD_POOL_KEEPALIVE_TIME,
        GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_ACTIVE_COUNT, GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_COMPLETED_TASK_COUNT,
        GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_CURRENT_THREAD_COUNT, GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_LARGEST_THREAD_COUNT,
        GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_TASK_COUNT, GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_KEEPALIVE_TIME
    };

    public static final MessagingSubsystemRootResourceDefinition INSTANCE = new MessagingSubsystemRootResourceDefinition();

    private MessagingSubsystemRootResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(MessagingExtension.SUBSYSTEM_PATH,
                MessagingExtension.getResourceDescriptionResolver(MessagingExtension.SUBSYSTEM_NAME))
                .setAddHandler(MessagingSubsystemAdd.INSTANCE)
                .setRemoveHandler(new ReloadRequiredRemoveStepHandler())
                .setCapabilities(CONFIGURATION_CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition metric : METRICS) {
            resourceRegistration.registerMetric(metric, ClientThreadPoolMetricReader.INSTANCE);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    private static final class ClientThreadPoolMetricReader implements OperationStepHandler {

        private static final ClientThreadPoolMetricReader INSTANCE = new ClientThreadPoolMetricReader();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            ThreadPoolExecutor pool;
            String metric;
            if (attributeName.startsWith(GLOBAL_CLIENT_PREFIX)) {
                pool = (ThreadPoolExecutor) ActiveMQClient.getGlobalThreadPool();
                metric = attributeName.substring(GLOBAL_CLIENT_PREFIX.length());
            } else {
                pool = (ThreadPoolExecutor) ActiveMQClient.getGlobalScheduledThreadPool();
                metric = attributeName.substring(GLOBAL_CLIENT_SCHEDULED_PREFIX.length());
            }
            switch (metric) {
                case org.jboss.as.threads.CommonAttributes.ACTIVE_COUNT:
                    context.getResult().set(pool.getActiveCount());
                    break;
                case org.jboss.as.threads.CommonAttributes.COMPLETED_TASK_COUNT:
                    context.getResult().set(pool.getCompletedTaskCount());
                    break;
                case org.jboss.as.threads.CommonAttributes.CURRENT_THREAD_COUNT:
                    context.getResult().set(pool.getPoolSize());
                    break;
                case org.jboss.as.threads.CommonAttributes.LARGEST_THREAD_COUNT:
                    context.getResult().set(pool.getLargestPoolSize());
                    break;
                case org.jboss.as.threads.CommonAttributes.TASK_COUNT:
                    context.getResult().set(pool.getTaskCount());
                    break;
                case org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME:
                    context.getResult().set(pool.getKeepAliveTime(TimeUnit.NANOSECONDS));
                    break;
                default:
                    // Programming bug. Throw a RuntimeException, not OFE, as this is not a client error
                    throw new IllegalArgumentException(metric);
            }
        }
    }

}
