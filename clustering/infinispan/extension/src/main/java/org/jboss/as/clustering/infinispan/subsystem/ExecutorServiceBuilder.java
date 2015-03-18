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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.as.threads.BoundedQueueThreadPoolService;
import org.jboss.as.threads.ManagedQueueExecutorService;
import org.jboss.as.threads.TimeSpec;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;

/**
 * @author Radoslav Husar
 * @version Mar 2015
 */
class ExecutorServiceBuilder extends ExecutorServiceNameBuilder implements Builder<ManagedQueueExecutorService> {

    private final String executorName;
    private int minThreads;
    private int maxThreads;
    private int queueLength;
    private long keepaliveTime;
    private ThreadFactoryServiceBuilder threadFactoryServiceBuilder;

    ExecutorServiceBuilder(final String cacheContainerName, final String executorName) {
        super(cacheContainerName);
        this.executorName = executorName;
    }

    @Override
    public ServiceBuilder<ManagedQueueExecutorService> build(ServiceTarget target) {
        BoundedQueueThreadPoolService service = new BoundedQueueThreadPoolService(minThreads, maxThreads, queueLength, true, new TimeSpec(TimeUnit.MILLISECONDS, keepaliveTime), true);

        return new AsynchronousServiceBuilder<>(this.getServiceName(), service).build(target)
                .addDependency(threadFactoryServiceBuilder.getServiceName(), ThreadFactory.class, service.getThreadFactoryInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public ServiceName getServiceName() {
        return super.getServiceName().append(executorName);
    }

    ExecutorServiceBuilder setMinThreads(final int minThreads) {
        this.minThreads = minThreads;
        return this;
    }

    ExecutorServiceBuilder setMaxThreads(final int maxThreads) {
        this.maxThreads = maxThreads;
        return this;
    }

    ExecutorServiceBuilder setQueueLength(final int queueLength) {
        this.queueLength = queueLength;
        return this;
    }

    ExecutorServiceBuilder setKeepaliveTime(final long keepaliveTime) {
        this.keepaliveTime = keepaliveTime;
        return this;
    }

    ExecutorServiceBuilder setThreadFactoryServiceBuilder(final ThreadFactoryServiceBuilder threadFactoryServiceBuilder) {
        this.threadFactoryServiceBuilder = threadFactoryServiceBuilder;
        return this;
    }
}
