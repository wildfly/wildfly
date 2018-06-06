/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.service;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.DelegatingServiceBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Paul Ferraro
 */
public class AsyncServiceConfigurator extends SimpleServiceNameProvider implements ServiceConfigurator {

    private static final ServiceName EXECUTOR_SERVICE_NAME = ServiceName.JBOSS.append("as", "server-executor");

    private volatile boolean asyncStart = true;
    private volatile boolean asyncStop = true;

    /**
     * Constructs a new builder for building asynchronous service
     * @param name the target service name
     */
    public AsyncServiceConfigurator(ServiceName name) {
        super(name);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Supplier<Executor> executor = builder.requires(EXECUTOR_SERVICE_NAME);
        return new AsyncServiceBuilder<>(builder, executor, this.asyncStart, this.asyncStop);
    }

    /**
     * Indicates that this service should *not* be started asynchronously.
     * @return a reference to this builder
     */
    public AsyncServiceConfigurator startSynchronously() {
        this.asyncStart = false;
        return this;
    }

    /**
     * Indicates that this service should *not* be stopped asynchronously.
     * @return a reference to this builder
     */
    public AsyncServiceConfigurator stopSynchronously() {
        this.asyncStop = false;
        return this;
    }

    private static class AsyncServiceBuilder<T> extends DelegatingServiceBuilder<T> {
        private final Supplier<Executor> executor;
        private final boolean asyncStart;
        private final boolean asyncStop;

        AsyncServiceBuilder(ServiceBuilder<T> delegate, Supplier<Executor> executor, boolean asyncStart, boolean asyncStop) {
            super(delegate);
            this.executor = executor;
            this.asyncStart = asyncStart;
            this.asyncStop = asyncStop;
        }

        @Override
        public ServiceBuilder<T> setInstance(Service service) {
            return super.setInstance(new AsyncService(service, this.executor, this.asyncStart, this.asyncStop));
        }
    }

    private static class AsyncService implements Service {
        private final Service service;
        private final Supplier<Executor> executor;
        private final boolean asyncStart;
        private final boolean asyncStop;

        AsyncService(Service service, Supplier<Executor> executor, boolean asyncStart, boolean asyncStop) {
            this.service = service;
            this.executor = executor;
            this.asyncStart = asyncStart;
            this.asyncStop = asyncStop;
        }

        @Override
        public void start(final StartContext context) throws StartException {
            if (this.asyncStart) {
                Runnable task = () -> {
                    try {
                        this.service.start(context);
                        context.complete();
                    } catch (StartException e) {
                        context.failed(e);
                    } catch (Throwable e) {
                        context.failed(new StartException(e));
                    }
                };
                try {
                    this.executor.get().execute(task);
                } catch (RejectedExecutionException e) {
                    task.run();
                } finally {
                    context.asynchronous();
                }
            } else {
                this.service.start(context);
            }
        }

        @Override
        public void stop(final StopContext context) {
            if (this.asyncStop) {
                Runnable task = () -> {
                    try {
                        this.service.stop(context);
                    } finally {
                        context.complete();
                    }
                };
                try {
                    this.executor.get().execute(task);
                } catch (RejectedExecutionException e) {
                    task.run();
                } finally {
                    context.asynchronous();
                }
            } else {
                this.service.stop(context);
            }
        }
    }
}
