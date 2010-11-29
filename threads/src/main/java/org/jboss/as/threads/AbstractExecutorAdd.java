/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractExecutorAdd extends AbstractThreadsSubsystemUpdate<Void> {

    private static final long serialVersionUID = 3661832034337989182L;

    private final String name;
    private final ScaledCount maxThreads;
    private final Map<String, String> properties = new HashMap<String, String>(0);

    private String threadFactory;
    private TimeSpec keepaliveTime;

    protected AbstractExecutorAdd(final String name, final ScaledCount maxThreads) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (maxThreads == null) {
            throw new IllegalArgumentException("maxThreads is null");
        }
        maxThreads.getScaledCount();
        this.name = name;
        this.maxThreads = maxThreads;
    }

    public String getThreadFactory() {
        return threadFactory;
    }

    public void setThreadFactory(final String threadFactory) {
        this.threadFactory = threadFactory;
    }

    public TimeSpec getKeepaliveTime() {
        return keepaliveTime;
    }

    public void setKeepaliveTime(final TimeSpec keepaliveTime) {
        this.keepaliveTime = keepaliveTime;
    }

    public ScaledCount getMaxThreads() {
        return maxThreads;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    protected <T> ServiceBuilder<T> addThreadFactoryDependency(final ServiceName serviceName, ServiceBuilder<T> serviceBuilder, Injector<ThreadFactory> injector, BatchBuilder builder) {
        final ServiceName threadFactoryName;
        if (threadFactory == null) {
            threadFactoryName = serviceName.append("thread-factory");
            builder.addService(threadFactoryName, new ThreadFactoryService());
        } else {
            threadFactoryName = ThreadsServices.threadFactoryName(threadFactory);
        }
        return serviceBuilder.addDependency(threadFactoryName, ThreadFactory.class, injector);
    }

    public final ExecutorRemove getCompensatingUpdate(final ThreadsSubsystemElement original) {
        return new ExecutorRemove(name);
    }
}
