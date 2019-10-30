/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ee.concurrent;

import static org.jboss.as.ee.concurrent.ControlPointUtils.doWrap;
import static org.jboss.as.ee.concurrent.SecurityIdentityUtils.doIdentityWrap;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;
import org.wildfly.extension.requestcontroller.ControlPoint;

/**
 * @author Stuart Douglas
 */
public class ManagedExecutorServiceImpl extends org.glassfish.enterprise.concurrent.ManagedExecutorServiceImpl {

    private final ControlPoint controlPoint;

    public ManagedExecutorServiceImpl(String name, ManagedThreadFactoryImpl managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, ContextServiceImpl contextService, RejectPolicy rejectPolicy, BlockingQueue<Runnable> queue, ControlPoint controlPoint) {
        super(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService, rejectPolicy, queue);
        this.controlPoint = controlPoint;
    }

    public ManagedExecutorServiceImpl(String name, ManagedThreadFactoryImpl managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, int queueCapacity, ContextServiceImpl contextService, RejectPolicy rejectPolicy, ControlPoint controlPoint) {
        super(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueCapacity, contextService, rejectPolicy);
        this.controlPoint = controlPoint;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(doIdentityWrap(doWrap(task, controlPoint)));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(doIdentityWrap(doWrap(task, controlPoint)), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(doIdentityWrap(doWrap(task, controlPoint)));
    }

    @Override
    public void execute(Runnable command) {
        super.execute(doIdentityWrap(doWrap(command, controlPoint)));
    }
}
