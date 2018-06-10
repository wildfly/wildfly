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

package org.wildfly.clustering.server.singleton;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Distributed {@link org.wildfly.clustering.singleton.service.SingletonService} implementation that uses JBoss MSC 1.4.x service installation.
 * @author Paul Ferraro
 */
public class DistributedSingletonService extends AbstractDistributedSingletonService<Lifecycle> {

    public DistributedSingletonService(DistributedSingletonServiceContext context, Service service, List<Map.Entry<ServiceName[], DeferredInjector<?>>> injectors) {
        super(context, new PrimaryServiceLifecycleFactory(context.getServiceName(), service, injectors));
    }

    @Override
    public Lifecycle get() {
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
