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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.resource.AbstractSocketBindingResourceDefinition;
import org.jboss.as.controller.transform.AttributeTransformationRequirementChecker;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.msc.service.ServiceName;

/**
 * {@link ResourceDefinition} for a bounded queue thread pool resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BoundedQueueThreadPoolResourceDefinition extends SimpleResourceDefinition {

    public static BoundedQueueThreadPoolResourceDefinition create(boolean blocking, boolean registerRuntimeOnly) {
        if (blocking) {
            return create(CommonAttributes.BLOCKING_BOUNDED_QUEUE_THREAD_POOL, ThreadsServices.STANDARD_THREAD_FACTORY_RESOLVER,
                    null, ThreadsServices.EXECUTOR, registerRuntimeOnly);
        } else {
            return create(CommonAttributes.BOUNDED_QUEUE_THREAD_POOL, ThreadsServices.STANDARD_THREAD_FACTORY_RESOLVER,
                    ThreadsServices.STANDARD_HANDOFF_EXECUTOR_RESOLVER, ThreadsServices.EXECUTOR, registerRuntimeOnly);
        }
    }

    public static BoundedQueueThreadPoolResourceDefinition create(boolean blocking, String type, boolean registerRuntimeOnly) {
        if (blocking) {
            return create(type, ThreadsServices.STANDARD_THREAD_FACTORY_RESOLVER, null, ThreadsServices.EXECUTOR, registerRuntimeOnly);
        } else {
            return create(type, ThreadsServices.STANDARD_THREAD_FACTORY_RESOLVER, ThreadsServices.STANDARD_HANDOFF_EXECUTOR_RESOLVER,
                    ThreadsServices.EXECUTOR, registerRuntimeOnly);
        }
    }

    public static BoundedQueueThreadPoolResourceDefinition create(String type, ThreadFactoryResolver threadFactoryResolver,
                                                                  HandoffExecutorResolver handoffExecutorResolver,
                                                                  ServiceName poolNameBase, boolean registerRuntimeOnly) {
        final boolean blocking = handoffExecutorResolver == null;
        final String resolverPrefix = blocking ? CommonAttributes.BLOCKING_BOUNDED_QUEUE_THREAD_POOL : CommonAttributes.BOUNDED_QUEUE_THREAD_POOL;
        final BoundedQueueThreadPoolAdd addHandler = new BoundedQueueThreadPoolAdd(blocking, threadFactoryResolver, handoffExecutorResolver, poolNameBase);
        final OperationStepHandler removeHandler = new BoundedQueueThreadPoolRemove(addHandler);
        return new BoundedQueueThreadPoolResourceDefinition(blocking, registerRuntimeOnly, type, poolNameBase, resolverPrefix, addHandler, removeHandler);
    }

    private final boolean registerRuntimeOnly;
    private final boolean blocking;
    private final ServiceName serviceNameBase;

    private BoundedQueueThreadPoolResourceDefinition(boolean blocking, boolean registerRuntimeOnly,
                                                     String type, ServiceName serviceNameBase, String resolverPrefix, OperationStepHandler addHandler,
                                                     OperationStepHandler removeHandler) {
        super(PathElement.pathElement(type),
                new ThreadPoolResourceDescriptionResolver(resolverPrefix, ThreadsExtension.RESOURCE_NAME, ThreadsExtension.class.getClassLoader()),
                addHandler, removeHandler);
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.blocking = blocking;
        this.serviceNameBase = serviceNameBase;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(PoolAttributeDefinitions.NAME, ReadResourceNameOperationStepHandler.INSTANCE);
        BoundedQueueThreadPoolWriteAttributeHandler writeHandler = new BoundedQueueThreadPoolWriteAttributeHandler(blocking, serviceNameBase);
        writeHandler.registerAttributes(resourceRegistration);
        if (registerRuntimeOnly) {
            new BoundedQueueThreadPoolMetricsHandler(serviceNameBase).registerAttributes(resourceRegistration);
        }
    }

    public static void registerTransformers1_0(TransformersSubRegistration parent) {
        registerTransformers1_0(parent, CommonAttributes.BLOCKING_BOUNDED_QUEUE_THREAD_POOL);
        registerTransformers1_0(parent, CommonAttributes.BOUNDED_QUEUE_THREAD_POOL);
    }

    public static void registerTransformers1_0(TransformersSubRegistration parent, String type) {

        Map<String, AttributeTransformationRequirementChecker> attributeCheckers = new HashMap<String, AttributeTransformationRequirementChecker>();
        attributeCheckers.put(PoolAttributeDefinitions.ALLOW_CORE_TIMEOUT.getName(), AttributeTransformationRequirementChecker.SIMPLE_EXPRESSIONS);
        attributeCheckers.put(PoolAttributeDefinitions.KEEPALIVE_TIME.getName(), KeepAliveTimeAttributeDefinition.TRANSFORMATION_REQUIREMENT_CHECKER);

        final RejectExpressionValuesTransformer TRANSFORMER = new RejectExpressionValuesTransformer(attributeCheckers);

        final TransformersSubRegistration pool = parent.registerSubResource(PathElement.pathElement(type), (ResourceTransformer) TRANSFORMER);
        pool.registerOperationTransformer(ADD, TRANSFORMER);
        pool.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, TRANSFORMER.getWriteAttributeTransformer());
    }
}
