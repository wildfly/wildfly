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
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * {@link ResourceDefinition} for the root resource of the threads subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class ThreadSubsystemResourceDefinition extends SimpleResourceDefinition {

    private final boolean registerRuntimeOnly;

    ThreadSubsystemResourceDefinition(boolean registerRuntimeOnly) {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ThreadsExtension.SUBSYSTEM_NAME),
                new StandardResourceDescriptionResolver(ThreadsExtension.SUBSYSTEM_NAME, ThreadsExtension.RESOURCE_NAME,
                        ThreadsExtension.class.getClassLoader(), true, false), ThreadsSubsystemAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE,
                OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_ALL_SERVICES);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {

        resourceRegistration.registerSubModel(new ThreadFactoryResourceDefinition());

        resourceRegistration.registerSubModel(QueuelessThreadPoolResourceDefinition.create(true, registerRuntimeOnly));
        resourceRegistration.registerSubModel(QueuelessThreadPoolResourceDefinition.create(false, registerRuntimeOnly));

        resourceRegistration.registerSubModel(BoundedQueueThreadPoolResourceDefinition.create(true, registerRuntimeOnly));
        resourceRegistration.registerSubModel(BoundedQueueThreadPoolResourceDefinition.create(false, registerRuntimeOnly));

        resourceRegistration.registerSubModel(new UnboundedQueueThreadPoolResourceDefinition(registerRuntimeOnly));

        resourceRegistration.registerSubModel(new ScheduledThreadPoolResourceDefinition(registerRuntimeOnly));
    }
}
