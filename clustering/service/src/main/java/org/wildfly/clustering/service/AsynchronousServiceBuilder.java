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
package org.wildfly.clustering.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Builder for asynchronously started/stopped services.
 * @author Paul Ferraro
 * @param <T> the type of value provided by services built by this builder
 * @deprecated Replaced by {@link AsyncServiceConfigurator}.
 */
@Deprecated
public class AsynchronousServiceBuilder<T> implements Builder<T>, Service<T> {

    private static final ServiceName EXECUTOR_SERVICE_NAME = ServiceName.JBOSS.append("as", "server-executor");

    private final InjectedValue<ExecutorService> executor = new InjectedValue<>();
    private final Service<T> service;
    private final ServiceName name;
    private volatile boolean startAsynchronously = true;
    private volatile boolean stopAsynchronously = true;

    /**
     * Constructs a new builder for building asynchronous service
     * @param name the target service name
     * @param service the target service
     */
    public AsynchronousServiceBuilder(ServiceName name, Service<T> service) {
        this.name = name;
        this.service = service;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public ServiceBuilder<T> build(ServiceTarget target) {
        return target.addService(this.name, this).addDependency(EXECUTOR_SERVICE_NAME, ExecutorService.class, this.executor);
    }

    /**
     * Indicates that this service should *not* be started asynchronously.
     * @return a reference to this builder
     */
    public AsynchronousServiceBuilder<T> startSynchronously() {
        this.startAsynchronously = false;
        return this;
    }

    /**
     * Indicates that this service should *not* be stopped asynchronously.
     * @return a reference to this builder
     */
    public AsynchronousServiceBuilder<T> stopSynchronously() {
        this.stopAsynchronously = false;
        return this;
    }

    @Override
    public T getValue() {
        return this.service.getValue();
    }

    @Override
    public void start(final StartContext context) throws StartException {
        if (this.startAsynchronously) {
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
                this.executor.getValue().execute(task);
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
        if (this.stopAsynchronously) {
            Runnable task = () -> {
                try {
                    this.service.stop(context);
                } finally {
                    context.complete();
                }
            };
            try {
                this.executor.getValue().execute(task);
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
