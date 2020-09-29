/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.microprofile.faulttolerance;

import io.opentracing.Tracer;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.InitialContext;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.smallrye.faulttolerance.ExecutorFactory;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import org.wildfly.extension.microprofile.faulttolerance.MiniContextPropagation.ContextProvider;

/**
 * Implementation of {@link io.smallrye.faulttolerance.ExecutorFactory} specific to this container to provide context
 * propagation capabilities.
 *
 * @author Radoslav Husar
 */
public class FaultToleranceContainerExecutorFactory implements ExecutorFactory {

    // Default in use by io.smallrye.faulttolerance.DefaultExecutorFactory
    private static final int KEEP_ALIVE_TIME = 600;

    @Override
    public ExecutorService createCoreExecutor(int size) {
        ExecutorService baseExecutor = new ThreadPoolExecutor(1, size, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new SynchronousQueue<>(), this.getThreadFactory());
        return MiniContextPropagation.executorService(getTracingContext(), baseExecutor);
    }

    @Override
    public ExecutorService createExecutor(int coreSize, int size) {
        ExecutorService baseExecutor = new ThreadPoolExecutor(coreSize, size, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), this.getThreadFactory());
        return MiniContextPropagation.executorService(getTracingContext(), baseExecutor);
    }

    @Override
    public ScheduledExecutorService createTimeoutExecutor(int size) {
        ScheduledExecutorService baseExecutor = Executors.newScheduledThreadPool(size, this.getThreadFactory());
        return MiniContextPropagation.scheduledExecutorService(getTracingContext(), baseExecutor);
    }

    private ContextProvider getTracingContext() {
        Instance<Tracer> tracerInstance = CDI.current().select(Tracer.class);
        if (tracerInstance.isResolvable()) {
            return new TracingContextProvider(tracerInstance.get());
        }
        return MiniContextPropagation.ContextProvider.NOOP;
    }

    private ThreadFactory getThreadFactory() {
        try {
            InitialContext initialContext = new InitialContext();
            return (ManagedThreadFactory) initialContext.lookup("java:jboss/ee/concurrency/factory/default");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int priority() {
        // Needs to be higher than io.smallrye.faulttolerance.DefaultExecutorFactory.priority()
        return 10;
    }
}
