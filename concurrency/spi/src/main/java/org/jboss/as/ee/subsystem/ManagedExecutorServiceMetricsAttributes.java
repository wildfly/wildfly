/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * The managed executor service resource's runtime metrics attribute names and definitions.
 * @author emmartins
 */
public interface ManagedExecutorServiceMetricsAttributes {

    String ACTIVE_THREAD_COUNT = "active-thread-count";
    String COMPLETED_TASK_COUNT = "completed-task-count";
    String CURRENT_QUEUE_SIZE = "current-queue-size";
    String HUNG_THREAD_COUNT = "hung-thread-count";
    String MAX_THREAD_COUNT = "max-thread-count";
    String TASK_COUNT = "task-count";
    String THREAD_COUNT = "thread-count";

    AttributeDefinition ACTIVE_THREAD_COUNT_AD = new SimpleAttributeDefinitionBuilder(ACTIVE_THREAD_COUNT, ModelType.INT)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .build();
    AttributeDefinition COMPLETED_TASK_COUNT_AD = new SimpleAttributeDefinitionBuilder(COMPLETED_TASK_COUNT, ModelType.LONG)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .build();
    AttributeDefinition CURRENT_QUEUE_SIZE_AD = new SimpleAttributeDefinitionBuilder(CURRENT_QUEUE_SIZE, ModelType.INT)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .build();
    AttributeDefinition HUNG_THREAD_COUNT_AD = new SimpleAttributeDefinitionBuilder(HUNG_THREAD_COUNT, ModelType.INT)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .build();
    AttributeDefinition MAX_THREAD_COUNT_AD = new SimpleAttributeDefinitionBuilder(MAX_THREAD_COUNT, ModelType.INT)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .build();
    AttributeDefinition TASK_COUNT_AD = new SimpleAttributeDefinitionBuilder(TASK_COUNT, ModelType.LONG)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .build();
    AttributeDefinition THREAD_COUNT_AD = new SimpleAttributeDefinitionBuilder(THREAD_COUNT, ModelType.INT)
            .setUndefinedMetricValue(ModelNode.ZERO)
            .build();
}
