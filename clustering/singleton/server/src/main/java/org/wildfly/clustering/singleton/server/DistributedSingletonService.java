/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.wildfly.clustering.singleton.Singleton;

/**
 * Distributed {@link org.wildfly.clustering.singleton.service.SingletonService} implementation that uses JBoss MSC 1.4.x service installation.
 * @author Paul Ferraro
 */
public class DistributedSingletonService extends AbstractDistributedSingletonService<SingletonContext> {

    private final Consumer<Singleton> singleton;

    public DistributedSingletonService(DistributedSingletonServiceContext context, Service service, Consumer<Singleton> singleton, List<Map.Entry<ServiceName[], DeferredInjector<?>>> injectors) {
        super(context, new PrimaryServiceLifecycleFactory(context.getServiceName(), service, injectors));
        this.singleton = singleton;
    }

    @Override
    public void start(StartContext context) throws StartException {
        super.start(context);
        this.singleton.accept(this);
    }

    @Override
    public SingletonContext get() {
        return this;
    }

    private static class PrimaryServiceLifecycleFactory implements Function<ServiceTarget, Lifecycle> {
        private final ServiceName name;
        private final Service service;
        private final List<Map.Entry<ServiceName[], DeferredInjector<?>>> injectors;

        PrimaryServiceLifecycleFactory(ServiceName name, Service service, List<Map.Entry<ServiceName[], DeferredInjector<?>>> injectors) {
            this.name = name;
            this.service = service;
            this.injectors = injectors;
        }

        @Override
        public Lifecycle apply(ServiceTarget target) {
            ServiceBuilder<?> builder = target.addService(this.name);
            for (Map.Entry<ServiceName[], DeferredInjector<?>> entry : this.injectors) {
                entry.getValue().setConsumer(builder.provides(entry.getKey()));
            }
            return new ServiceLifecycle(builder.setInstance(this.service).setInitialMode(ServiceController.Mode.NEVER).install());
        }
    }
}
