/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.service.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.JBossExecutors;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.SuppliedValueService;

/**
 * Service that provides an {@link Executor} that uses a cached thread pool.
 * @author Paul Ferraro
 */
public class CachedThreadPoolExecutorServiceBuilder implements Builder<ExecutorService>, Function<ExecutorService, ExecutorService>, Supplier<ExecutorService>, Consumer<ExecutorService> {

    private final ServiceName name;
    private final ThreadFactory factory;

    public CachedThreadPoolExecutorServiceBuilder(ServiceName name, ThreadFactory factory) {
        this.name = name;
        this.factory = factory;
    }

    @Override
    public ExecutorService apply(ExecutorService executor) {
        return JBossExecutors.protectedExecutorService(executor);
    }

    @Override
    public ExecutorService get() {
        return Executors.newCachedThreadPool(this.factory);
    }

    @Override
    public void accept(ExecutorService executor) {
        executor.shutdownNow();
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public ServiceBuilder<ExecutorService> build(ServiceTarget target) {
        Service<ExecutorService> service = new SuppliedValueService<>(this, this, this);
        return new AsynchronousServiceBuilder<>(this.name, service).startSynchronously().build(target).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
