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
 * {@link ResourceDefinition} for a queueless thread pool resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class QueuelessThreadPoolResourceDefinition extends SimpleResourceDefinition {

    public static QueuelessThreadPoolResourceDefinition create(boolean blocking, boolean registerRuntimeOnly) {
        if (blocking) {
            return create(CommonAttributes.BLOCKING_QUEUELESS_THREAD_POOL, ThreadsServices.STANDARD_THREAD_FACTORY_RESOLVER,
                    null, ThreadsServices.EXECUTOR, registerRuntimeOnly);
        } else {
            return create(CommonAttributes.QUEUELESS_THREAD_POOL, ThreadsServices.STANDARD_THREAD_FACTORY_RESOLVER,
                    ThreadsServices.STANDARD_HANDOFF_EXECUTOR_RESOLVER, ThreadsServices.EXECUTOR, registerRuntimeOnly);
        }
    }

    public static QueuelessThreadPoolResourceDefinition create(boolean blocking, String type, boolean registerRuntimeOnly) {
        if (blocking) {
            return create(type, ThreadsServices.STANDARD_THREAD_FACTORY_RESOLVER,
                    null, ThreadsServices.EXECUTOR, registerRuntimeOnly);

        } else {
            return create(type, ThreadsServices.STANDARD_THREAD_FACTORY_RESOLVER,
                    ThreadsServices.STANDARD_HANDOFF_EXECUTOR_RESOLVER, ThreadsServices.EXECUTOR, registerRuntimeOnly);
        }
    }

    public static QueuelessThreadPoolResourceDefinition create(String type, ThreadFactoryResolver threadFactoryResolver,
                                                               HandoffExecutorResolver handoffExecutorResolver,
                                                               ServiceName serviceNameBase, boolean registerRuntimeOnly) {
        final boolean blocking = handoffExecutorResolver == null;
        final String resolverPrefix = blocking ? CommonAttributes.BLOCKING_QUEUELESS_THREAD_POOL : CommonAttributes.QUEUELESS_THREAD_POOL;
        final QueuelessThreadPoolAdd addHandler = new QueuelessThreadPoolAdd(blocking, threadFactoryResolver, handoffExecutorResolver, serviceNameBase);
        final OperationStepHandler removeHandler = new QueuelessThreadPoolRemove(addHandler);
        return new QueuelessThreadPoolResourceDefinition(blocking, registerRuntimeOnly, type, serviceNameBase, resolverPrefix, addHandler, removeHandler);
    }

    private final boolean registerRuntimeOnly;
    private final boolean blocking;
    private final ServiceName serviceNameBase;

    private QueuelessThreadPoolResourceDefinition(boolean blocking, boolean registerRuntimeOnly,
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
        QueuelessThreadPoolWriteAttributeHandler writeHandler = new QueuelessThreadPoolWriteAttributeHandler(blocking, serviceNameBase);
        writeHandler.registerAttributes(resourceRegistration);
        if (registerRuntimeOnly) {
            new QueuelessThreadPoolMetricsHandler(serviceNameBase).registerAttributes(resourceRegistration);
        }
    }
}
