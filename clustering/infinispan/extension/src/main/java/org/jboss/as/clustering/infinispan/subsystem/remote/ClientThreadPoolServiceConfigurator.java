/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfiguration;
import org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory;
import org.infinispan.commons.executors.ExecutorFactory;
import org.jboss.as.clustering.infinispan.subsystem.ComponentServiceConfigurator;
import org.jboss.as.clustering.infinispan.subsystem.ThreadPoolDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.concurrent.ClassLoaderThreadFactory;

/**
 * @author Radoslav Husar
 */
public class ClientThreadPoolServiceConfigurator extends ComponentServiceConfigurator<ExecutorFactoryConfiguration> implements ThreadFactory {

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

        this.factory = new ExecutorFactory() {
            @Override
            public ExecutorService getExecutor(Properties property) {
                ThreadFactory clThreadFactory = new ClassLoaderThreadFactory(ClientThreadPoolServiceConfigurator.this, AccessController.doPrivileged((PrivilegedAction<ClassLoader>) ClassLoaderThreadFactory.class::getClassLoader));

                return new ThreadPoolExecutor(minThreads, maxThreads, keepAliveTime, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueLength), clThreadFactory);
            }
        };

        return this;
    }

    @Override
    public ExecutorFactoryConfiguration get() {
        return new ConfigurationBuilder().asyncExecutorFactory().factory(this.factory).create();
    }

    @Override
    public Thread newThread(Runnable task) {
        Thread thread = new Thread(task, DefaultAsyncExecutorFactory.THREAD_NAME + "-" + DefaultAsyncExecutorFactory.counter.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }
}

