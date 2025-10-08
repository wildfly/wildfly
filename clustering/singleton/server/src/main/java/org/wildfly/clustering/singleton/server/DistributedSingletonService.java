/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.server.service.Service;
import org.wildfly.clustering.singleton.Singleton;

/**
 * Distributed singleton service implementation that uses JBoss MSC 1.4.x service installation.
 * @author Paul Ferraro
 */
public class DistributedSingletonService extends AbstractSingletonService<SingletonContext, Service> {

    public DistributedSingletonService(SingletonServiceContext context, Function<ServiceTarget, ServiceBuilder<?>> builderFactory, List<Map.Entry<ServiceName[], Consumer<Consumer<?>>>> injectors, Consumer<Singleton> singleton) {
        super(context, new ServiceFactory(context.getService(), builderFactory, injectors), DefaultSingletonContext::new, singleton);
    }

    private static class ServiceFactory implements Function<ServiceTarget, Service> {
        private final org.jboss.msc.Service service;
        private final Function<ServiceTarget, ServiceBuilder<?>> builderFactory;
        private final List<Map.Entry<ServiceName[], Consumer<Consumer<?>>>> injectors;

        ServiceFactory(org.jboss.msc.Service service, Function<ServiceTarget, ServiceBuilder<?>> builderFactory, List<Map.Entry<ServiceName[], Consumer<Consumer<?>>>> injectors) {
            this.service = service;
            this.builderFactory = builderFactory;
            this.injectors = injectors;
        }

        @Override
        public Service apply(ServiceTarget target) {
            ServiceBuilder<?> builder = this.builderFactory.apply(target);
            for (Map.Entry<ServiceName[], Consumer<Consumer<?>>> entry : this.injectors) {
                entry.getValue().accept(builder.provides(entry.getKey()));
            }
            return new ServiceControllerService(builder.setInstance(this.service).setInitialMode(ServiceController.Mode.NEVER).install());
        }
    }
}
