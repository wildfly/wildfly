/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.infinispan.commons.executors.ThreadPoolExecutorFactory;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.ThreadPoolConfigurationBuilder;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.infinispan.ClassLoaderThreadFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.Builder;

/**
 * @author Radoslav Husar
 * @version August 2015
 */
public class ScheduledThreadPoolBuilder extends ComponentBuilder<ThreadPoolConfiguration> implements ResourceServiceBuilder<ThreadPoolConfiguration> {

    private final ThreadPoolConfigurationBuilder builder = new ThreadPoolConfigurationBuilder(null);
    private final ScheduledThreadPoolDefinition definition;

    ScheduledThreadPoolBuilder(ScheduledThreadPoolDefinition definition, PathAddress containerAddress) {
        super(definition, containerAddress);
        this.definition = definition;
    }

    @Override
    public Builder<ThreadPoolConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {

        int maxThreads = this.definition.getMaxThreads().resolveModelAttribute(context, model).asInt();
        long keepAliveTime = this.definition.getKeepAliveTime().resolveModelAttribute(context, model).asLong();

        ThreadPoolExecutorFactory<?> factory = new ThreadPoolExecutorFactory<ScheduledExecutorService>() {
            @Override
            public ScheduledExecutorService createExecutor(ThreadFactory factory) {
                // Use fixed size, based on maxThreads
                ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(maxThreads, new ClassLoaderThreadFactory(factory, ClassLoaderThreadFactory.class.getClassLoader()));
                executor.setKeepAliveTime(keepAliveTime, TimeUnit.MILLISECONDS);
                executor.setRemoveOnCancelPolicy(true);
                executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
                return executor;
            }

            @Override
            public void validate() {
                // Do nothing
            }
        };
        this.builder.threadPoolFactory(factory);

        return this;
    }

    @Override
    public ThreadPoolConfiguration getValue() {
        return this.builder.create();
    }
}

