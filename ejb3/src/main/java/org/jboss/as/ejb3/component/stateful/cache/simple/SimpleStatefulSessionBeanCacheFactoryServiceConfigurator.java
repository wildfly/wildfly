/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
