/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.threads.ThreadFactoryResolver;
import org.jboss.as.threads.ThreadFactoryResourceDefinition;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.threads.UnboundedQueueThreadPoolResourceDefinition;
import org.wildfly.extension.batch.services.BatchServiceNames;

public class BatchSubsystemDefinition extends SimpleResourceDefinition {

    /**
     * The name of our subsystem within the model.
     */
    public static final String NAME = "batch";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, NAME);
    private static final String RESOURCE_NAME = BatchSubsystemExtension.class.getPackage().getName() + ".LocalDescriptions";


    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, BatchSubsystemExtension.class.getClassLoader(), true, false);
    }

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... prefixes) {
        final StringBuilder prefix = new StringBuilder(NAME);
        for (String p : prefixes) {
            prefix.append('.').append(p);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, BatchSubsystemExtension.class.getClassLoader(), true, false);
    }

    public static final BatchSubsystemDefinition INSTANCE = new BatchSubsystemDefinition();

    private BatchSubsystemDefinition() {
        super(SUBSYSTEM_PATH, getResourceDescriptionResolver((String) null), BatchSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerChildren(final ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        // thread-pool resource
        final UnboundedQueueThreadPoolResourceDefinition threadPoolResource = UnboundedQueueThreadPoolResourceDefinition.create(
                PathElement.pathElement(BatchConstants.THREAD_POOL, BatchConstants.THREAD_POOL_NAME),
                BatchThreadFactoryResolver.INSTANCE, BatchServiceNames.BASE_BATCH_THREAD_POOL_NAME, false); // TODO (jrp) verify false value
        resourceRegistration.registerSubModel(threadPoolResource);

        // thread-factory resource
        final ThreadFactoryResourceDefinition threadFactoryResource = new ThreadFactoryResourceDefinition();
        resourceRegistration.registerSubModel(threadFactoryResource);

        resourceRegistration.registerSubModel(JobRepositoryDefinition.IN_MEMORY);
    }


    private static class BatchThreadFactoryResolver extends ThreadFactoryResolver.SimpleResolver {
        static final BatchThreadFactoryResolver INSTANCE = new BatchThreadFactoryResolver();

        private BatchThreadFactoryResolver() {
            super(ThreadsServices.FACTORY);
        }

        @Override
        protected String getThreadGroupName(String threadPoolName) {
            return "batch-" + threadPoolName;
        }
    }
}
