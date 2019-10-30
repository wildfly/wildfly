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

package org.wildfly.clustering.service.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.JBossExecutors;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * @author Paul Ferraro
 */
public class CachedThreadPoolExecutorServiceConfigurator extends SimpleServiceNameProvider implements ServiceConfigurator, Function<ExecutorService, ExecutorService>, Supplier<ExecutorService>, Consumer<ExecutorService> {

    private final ThreadFactory factory;

    public CachedThreadPoolExecutorServiceConfigurator(ServiceName name, ThreadFactory factory) {
        super(name);
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
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(this.getServiceName()).startSynchronously().build(target);
        Consumer<ExecutorService> executor = builder.provides(this.getServiceName());
        Service service = new FunctionalService<>(executor, this, this, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
