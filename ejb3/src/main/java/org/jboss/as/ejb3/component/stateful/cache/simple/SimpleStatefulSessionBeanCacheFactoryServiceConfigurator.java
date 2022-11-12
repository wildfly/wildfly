/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.stateful.cache.simple;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCache;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheConfiguration;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheFactory;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstanceFactory;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Configures a service that provides a simple stateful session bean cache factory.
 *
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class SimpleStatefulSessionBeanCacheFactoryServiceConfigurator<K, V extends StatefulSessionBeanInstance<K>> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, StatefulSessionBeanCacheFactory<K, V> {

    private final SupplierDependency<ServerEnvironment> environment = new ServiceSupplierDependency<>(ServerEnvironmentService.SERVICE_NAME);

    public SimpleStatefulSessionBeanCacheFactoryServiceConfigurator(StatefulComponentDescription description) {
        super(description.getCacheFactoryServiceName());
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<StatefulSessionBeanCacheFactory<K, V>> factory = this.environment.register(builder).provides(name);
        Service service = Service.newInstance(factory, this);
        return builder.setInstance(service);
    }

    @Override
    public StatefulSessionBeanCache<K, V> createStatefulBeanCache(StatefulSessionBeanCacheConfiguration<K, V> configuration) {
        ServerEnvironment environment = this.environment.get();
        return new SimpleStatefulSessionBeanCache<>(new SimpleStatefulSessionBeanCacheConfiguration<>() {
            @Override
            public StatefulSessionBeanInstanceFactory<V> getInstanceFactory() {
                return configuration.getInstanceFactory();
            }

            @Override
            public Supplier<K> getIdentifierFactory() {
                return configuration.getIdentifierFactory();
            }

            @Override
            public Duration getTimeout() {
                return configuration.getTimeout();
            }

            @Override
            public ServerEnvironment getEnvironment() {
                return environment;
            }

            @Override
            public String getComponentName() {
                return configuration.getComponentName();
            }
        });
    }
}
