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
package org.jboss.as.threads;


import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Attribute definitions used by thread pool resources.
 *
 * @author <a href="alex@jboss.org">Alexey Loubyansky</a>
 */
public interface PoolAttributeDefinitions {

    SimpleAttributeDefinition NAME = new SimpleAttributeDefinition(CommonAttributes.NAME, ModelType.STRING, true);

    SimpleAttributeDefinition THREAD_FACTORY = new SimpleAttributeDefinitionBuilder(CommonAttributes.THREAD_FACTORY, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    SimpleAttributeDefinition MAX_THREADS = new SimpleAttributeDefinitionBuilder(CommonAttributes.MAX_THREADS, ModelType.INT, false)
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, false, true)).setAllowExpression(true).build();

    SimpleAttributeDefinition KEEPALIVE_TIME = new KeepAliveTimeAttributeDefinition();

    SimpleAttributeDefinition CORE_THREADS = new SimpleAttributeDefinitionBuilder(CommonAttributes.CORE_THREADS, ModelType.INT, true)
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, true)).setAllowExpression(true).build();

    SimpleAttributeDefinition HANDOFF_EXECUTOR = new SimpleAttributeDefinitionBuilder(CommonAttributes.HANDOFF_EXECUTOR, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    SimpleAttributeDefinition QUEUE_LENGTH = new SimpleAttributeDefinitionBuilder(CommonAttributes.QUEUE_LENGTH, ModelType.INT, false)
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, false, true)).setAllowExpression(true).setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    SimpleAttributeDefinition ALLOW_CORE_TIMEOUT = new SimpleAttributeDefinitionBuilder(CommonAttributes.ALLOW_CORE_TIMEOUT, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false)).build();

    SimpleAttributeDefinition GROUP_NAME = new SimpleAttributeDefinition(CommonAttributes.GROUP_NAME, ModelType.STRING, true);

    SimpleAttributeDefinition THREAD_NAME_PATTERN = new SimpleAttributeDefinition(CommonAttributes.THREAD_NAME_PATTERN, ModelType.STRING, true);

    SimpleAttributeDefinition PRIORITY = new SimpleAttributeDefinition(CommonAttributes.PRIORITY, CommonAttributes.PRIORITY, new ModelNode().set(-1),
            ModelType.INT, true, true, MeasurementUnit.NONE, new IntRangeValidator(-1, 10, true, true));

    // Metrics

    AttributeDefinition CURRENT_THREAD_COUNT = new SimpleAttributeDefinition(CommonAttributes.CURRENT_THREAD_COUNT, ModelType.INT, false);
    AttributeDefinition LARGEST_THREAD_COUNT = new SimpleAttributeDefinition(CommonAttributes.LARGEST_THREAD_COUNT, ModelType.INT, false);
    AttributeDefinition REJECTED_COUNT = new SimpleAttributeDefinition(CommonAttributes.REJECTED_COUNT, ModelType.INT, false);
    AttributeDefinition ACTIVE_COUNT = new SimpleAttributeDefinition(CommonAttributes.ACTIVE_COUNT, ModelType.INT, false);
    AttributeDefinition COMPLETED_TASK_COUNT = new SimpleAttributeDefinition(CommonAttributes.COMPLETED_TASK_COUNT, ModelType.INT, false);
    AttributeDefinition TASK_COUNT = new SimpleAttributeDefinition(CommonAttributes.TASK_COUNT, ModelType.INT, false);
}
