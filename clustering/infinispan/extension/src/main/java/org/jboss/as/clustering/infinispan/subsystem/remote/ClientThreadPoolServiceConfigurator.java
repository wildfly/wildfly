/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfiguration;
import org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory;
import org.infinispan.commons.executors.ExecutorFactory;
import org.infinispan.commons.util.concurrent.BlockingRejectedExecutionHandler;
import org.infinispan.commons.util.concurrent.NonBlockingRejectedExecutionHandler;
import org.jboss.as.clustering.infinispan.executors.DefaultNonBlockingThreadFactory;
import org.jboss.as.clustering.infinispan.subsystem.ComponentServiceConfigurator;
import org.jboss.as.clustering.infinispan.subsystem.ThreadPoolDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Radoslav Husar
 */
public class ClientThreadPoolServiceConfigurator extends ComponentServiceConfigurator<ExecutorFactoryConfiguration> {
    private static final ThreadFactory THREAD_FACTORY = new DaemonThreadFactory(DefaultAsyncExecutorFactory.THREAD_NAME);

    private final ThreadPoolDefinition definition;

    private volatile ExecutorFactory factory;

    public ClientThreadPoolServiceConfigurator(ThreadPoolDefinition definition, PathAddress address) {
        super(definition, address);
        this.definition = definition;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        int maxThreads = this.definition.getMaxThreads().resolveModelAttribute(context, model).asInt();
        int minThreads = this.definition.getMinThreads().resolveModelAttribute(context, model).asInt();
        int queueLength = this.definition.getQueueLength().resolveModelAttribute(context, model).asInt();
        long keepAliveTime = this.definition.getKeepAliveTime().resolveModelAttribute(context, model).asLong();
        boolean nonBlocking = this.definition.isNonBlocking();

        this.factory = new ExecutorFactory() {
            @Override
            public ExecutorService getExecutor(Properties property) {
                BlockingQueue<Runnable> queue = queueLength == 0 ? new SynchronousQueue<>() : new LinkedBlockingQueue<>(queueLength);
                ThreadFactory factory = new DefaultThreadFactory(THREAD_FACTORY);
                if (nonBlocking) {
                    factory = new DefaultNonBlockingThreadFactory(factory);
                }
                RejectedExecutionHandler handler = nonBlocking ? NonBlockingRejectedExecutionHandler.getInstance() : BlockingRejectedExecutionHandler.getInstance();

                return new ThreadPoolExecutor(minThreads, maxThreads, keepAliveTime, TimeUnit.MILLISECONDS, queue, factory, handler);
            }
        };

        return this;
    }

    @Override
    public ExecutorFactoryConfiguration get() {
        return new ConfigurationBuilder().asyncExecutorFactory().factory(this.factory).create();
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger index = new AtomicInteger(0);
        private final String name;

        DaemonThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, String.join("-", this.name, String.valueOf(this.index.getAndIncrement())));
            thread.setDaemon(true);
            return thread;
        }
    }
}

