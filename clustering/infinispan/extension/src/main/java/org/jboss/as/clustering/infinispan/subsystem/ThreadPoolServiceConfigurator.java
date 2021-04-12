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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import org.infinispan.commons.util.ProcessorInfo;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.ThreadPoolConfigurationBuilder;
import org.infinispan.factories.threads.EnhancedQueueExecutorFactory;
import org.jboss.as.clustering.context.DefaultThreadFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.threads.management.ManageableThreadPoolExecutorService;
import org.wildfly.clustering.infinispan.spi.DefaultNonBlockingThreadFactory;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Configures a service providing a {@link ThreadPoolConfiguration}.
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public class ThreadPoolServiceConfigurator extends GlobalComponentServiceConfigurator<ThreadPoolConfiguration> {

    private final ThreadPoolConfigurationBuilder builder = new ThreadPoolConfigurationBuilder(null);
    private final ThreadPoolDefinition definition;

    ThreadPoolServiceConfigurator(ThreadPoolDefinition definition, PathAddress address) {
        super(definition, address);
        this.definition = definition;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        int minThreads = this.definition.getMinThreads().resolveModelAttribute(context, model).asInt();
        int maxThreads = this.definition.getMaxThreads().resolveModelAttribute(context, model).asInt();
        int queueLength = this.definition.getQueueLength().resolveModelAttribute(context, model).asInt();
        long keepAliveTime = this.definition.getKeepAliveTime().resolveModelAttribute(context, model).asLong();
        boolean nonBlocking = this.definition.isNonBlocking();
        if (this.definition == ThreadPoolResourceDefinition.NON_BLOCKING) {
            int availableProcessors = ProcessorInfo.availableProcessors();
            minThreads *= availableProcessors;
            maxThreads *= availableProcessors;
        }
        this.builder.threadPoolFactory(nonBlocking ? new NonBlockingThreadPoolExecutorFactory(maxThreads, minThreads, queueLength, keepAliveTime) : new BlockingThreadPoolExecutorFactory(maxThreads, minThreads, queueLength, keepAliveTime));

        return this;
    }

    @Override
    public ThreadPoolConfiguration get() {
        return this.builder.create();
    }

    private static class BlockingThreadPoolExecutorFactory extends EnhancedQueueExecutorFactory {

        BlockingThreadPoolExecutorFactory(int maxThreads, int coreThreads, int queueLength, long keepAlive) {
            super(maxThreads, coreThreads, queueLength, keepAlive);
        }

        @Override
        public ManageableThreadPoolExecutorService createExecutor(ThreadFactory factory) {
            return super.createExecutor(new DefaultThreadFactory(factory));
        }
    }

    private static class NonBlockingThreadPoolExecutorFactory extends org.infinispan.factories.threads.NonBlockingThreadPoolExecutorFactory {

        NonBlockingThreadPoolExecutorFactory(int maxThreads, int coreThreads, int queueLength, long keepAlive) {
            super(maxThreads, coreThreads, queueLength, keepAlive);
        }

        @Override
        public ExecutorService createExecutor(ThreadFactory factory) {
            return super.createExecutor(new DefaultNonBlockingThreadFactory(factory));
        }
    }
}

