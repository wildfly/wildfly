/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.msc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.as.server.Services;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

/**
 * Service decorator that optionally starts/stops a service asynchronously.
 * @author Paul Ferraro
 */
public final class AsynchronousService<T> implements Service<T> {
    private static final boolean DEFAULT_ASYNC_START = true;
    private static final boolean DEFAULT_ASYNC_STOP = true;

    final Service<T> service;
    private final Value<ExecutorService> executor;
    private final boolean startAsynchronously;
    private final boolean stopAsynchronously;

    public static <T> ServiceBuilder<T> addService(ServiceTarget target, ServiceName name, Service<T> service) {
        return addService(target, name, service, DEFAULT_ASYNC_START, DEFAULT_ASYNC_STOP);
    }

    public static <T> ServiceBuilder<T> addService(ServiceTarget target, ServiceName name, Service<T> service, boolean startAsynchronously, boolean stopAsynchronously) {
        final InjectedValue<ExecutorService> executor = new InjectedValue<>();
        final ServiceBuilder<T> builder = target.addService(name, new AsynchronousService<>(service, executor, startAsynchronously, stopAsynchronously));
        Services.addServerExecutorDependency(builder, executor, false);
        return builder;
    }

    public AsynchronousService(Service<T> service, Value<ExecutorService> executor) {
        this(service, executor, DEFAULT_ASYNC_START, DEFAULT_ASYNC_STOP);
    }

    public AsynchronousService(Service<T> service, Value<ExecutorService> executor, boolean startAsynchronously, boolean stopAsynchronously) {
        this.service = service;
        this.executor = executor;
        this.startAsynchronously = startAsynchronously;
        this.stopAsynchronously = stopAsynchronously;
    }

    @Override
    public T getValue() {
        return this.service.getValue();
    }

    @Override
    public void start(final StartContext context) throws StartException {
        if (this.startAsynchronously) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        AsynchronousService.this.service.start(context);
                        context.complete();
                    } catch (StartException e) {
                        context.failed(e);
                    } catch (Throwable e) {
                        context.failed(new StartException(e));
                    }
                }
            };
            context.asynchronous();
            try {
                this.executor.getValue().execute(task);
            } catch (RejectedExecutionException e) {
                task.run();
            }
        } else {
            this.service.start(context);
        }
    }

    @Override
    public void stop(final StopContext context) {
        if (this.stopAsynchronously) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        AsynchronousService.this.service.stop(context);
                    } finally {
                        context.complete();
                    }
                }
            };
            context.asynchronous();
            try {
                this.executor.getValue().execute(task);
            } catch (RejectedExecutionException e) {
                task.run();
            }
        } else {
            this.service.stop(context);
        }
    }
}
