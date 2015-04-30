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

import org.jboss.as.threads.ManagedScheduledExecutorService;
import org.jboss.as.threads.ScheduledThreadPoolService;
import org.jboss.as.threads.TimeSpec;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Builder;

/**
 * Builder wrapping threads subsystem {@link ScheduledThreadPoolService}.
 *
 * @author Radoslav Husar
 * @version Mar 2015
 */
class ScheduledExecutorServiceBuilder extends ExecutorServiceNameBuilder implements Builder<ManagedScheduledExecutorService> {

    private final String executorName;
    private int maxThreads;
    private long keepaliveTime;
    private ThreadFactoryServiceBuilder threadFactoryServiceBuilder;

    ScheduledExecutorServiceBuilder(String cacheContainerName, String executorName) {
        super(cacheContainerName);
        this.executorName = executorName;
    }

    @Override
    public ServiceBuilder<ManagedScheduledExecutorService> build(ServiceTarget target) {
        ScheduledThreadPoolService service = new ScheduledThreadPoolService(maxThreads, new TimeSpec(TimeUnit.MILLISECONDS, keepaliveTime));

        // AsynchronousServiceBuilder cannot be used as the wrapped services are already stopped asynchronously.
        return target.addService(this.getServiceName(), service)
                .addDependency(threadFactoryServiceBuilder.getServiceName(), ThreadFactory.class, service.getThreadFactoryInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }


    @Override
    public ServiceName getServiceName() {
        return super.getServiceName().append(executorName);
    }

    ScheduledExecutorServiceBuilder setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        return this;
    }

    ScheduledExecutorServiceBuilder setKeepaliveTime(long keepaliveTime) {
        this.keepaliveTime = keepaliveTime;
        return this;
    }

    ScheduledExecutorServiceBuilder setThreadFactoryServiceBuilder(final ThreadFactoryServiceBuilder threadFactoryServiceBuilder) {
        this.threadFactoryServiceBuilder = threadFactoryServiceBuilder;
        return this;
    }
}
