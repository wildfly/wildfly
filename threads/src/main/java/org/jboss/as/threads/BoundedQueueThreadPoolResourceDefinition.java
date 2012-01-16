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

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.msc.service.ServiceName;

/**
 * {@link ResourceDefinition} for a bounded queue thread pool resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BoundedQueueThreadPoolResourceDefinition extends SimpleResourceDefinition {

    public static BoundedQueueThreadPoolResourceDefinition create(boolean blocking, boolean registerRuntimeOnly) {
        return create(blocking, DefaultThreadFactoryProvider.STANDARD_PROVIDER, ThreadsServices.EXECUTOR, registerRuntimeOnly);
    }

    public static BoundedQueueThreadPoolResourceDefinition create(boolean blocking, String type, boolean registerRuntimeOnly) {
        return create(blocking, type, DefaultThreadFactoryProvider.STANDARD_PROVIDER, ThreadsServices.EXECUTOR, registerRuntimeOnly);
    }

    public static BoundedQueueThreadPoolResourceDefinition create(boolean blocking, DefaultThreadFactoryProvider defaultThreadFactoryProvider,
                                                                  ServiceName poolNameBase, boolean registerRuntimeOnly) {
        final String type = blocking ? CommonAttributes.BLOCKING_BOUNDED_QUEUE_THREAD_POOL : CommonAttributes.BOUNDED_QUEUE_THREAD_POOL;
        return create(blocking, type, defaultThreadFactoryProvider, poolNameBase, registerRuntimeOnly);
    }

    public static BoundedQueueThreadPoolResourceDefinition create(boolean blocking, String type, DefaultThreadFactoryProvider defaultThreadFactoryProvider,
                                                                  ServiceName poolNameBase, boolean registerRuntimeOnly) {
        final String resolverPrefix = blocking ? CommonAttributes.BLOCKING_BOUNDED_QUEUE_THREAD_POOL : CommonAttributes.BOUNDED_QUEUE_THREAD_POOL;
        final BoundedQueueThreadPoolAdd addHandler = new BoundedQueueThreadPoolAdd(blocking, defaultThreadFactoryProvider, poolNameBase);
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
        resourceRegistration.registerReadOnlyAttribute(PoolAttributeDefinitions.NAME, null);
        BoundedQueueThreadPoolWriteAttributeHandler writeHandler = new BoundedQueueThreadPoolWriteAttributeHandler(blocking, serviceNameBase);
        writeHandler.registerAttributes(resourceRegistration);
        if (registerRuntimeOnly) {
            new BoundedQueueThreadPoolMetricsHandler(serviceNameBase).registerAttributes(resourceRegistration);
        }
    }
}
