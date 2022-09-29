/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
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
