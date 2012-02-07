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

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.msc.service.ServiceName;

/**
 * {@link ResourceDefinition} for a scheduled thread pool resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ScheduledThreadPoolResourceDefinition extends SimpleResourceDefinition {

    private final boolean registerRuntimeOnly;
    private final ServiceName serviceNameBase;

    public static ScheduledThreadPoolResourceDefinition create(boolean registerRuntimeOnly) {
        return create(CommonAttributes.SCHEDULED_THREAD_POOL, ThreadsServices.STANDARD_THREAD_FACTORY_RESOLVER, ThreadsServices.EXECUTOR, registerRuntimeOnly);
    }
    public static ScheduledThreadPoolResourceDefinition create(String type, ThreadFactoryResolver threadFactoryResolver,
                                                 ServiceName serviceNameBase, boolean registerRuntimeOnly) {
        ScheduledThreadPoolAdd addHandler = new ScheduledThreadPoolAdd(threadFactoryResolver, serviceNameBase);
        return new ScheduledThreadPoolResourceDefinition(type, addHandler, serviceNameBase, registerRuntimeOnly);
    }

    private ScheduledThreadPoolResourceDefinition(String type, ScheduledThreadPoolAdd addHandler,
                                                 ServiceName serviceNameBase, boolean registerRuntimeOnly) {
        super(PathElement.pathElement(type),
                new ThreadPoolResourceDescriptionResolver(CommonAttributes.SCHEDULED_THREAD_POOL, ThreadsExtension.RESOURCE_NAME,
                ThreadsExtension.class.getClassLoader()),
                addHandler, new ScheduledThreadPoolRemove(addHandler));
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.serviceNameBase = serviceNameBase;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(PoolAttributeDefinitions.NAME, null);
        new ScheduledThreadPoolWriteAttributeHandler(serviceNameBase).registerAttributes(resourceRegistration);
        if (registerRuntimeOnly) {
            new ScheduledThreadPoolMetricsHandler(serviceNameBase).registerAttributes(resourceRegistration);
        }
    }
}
