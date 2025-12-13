/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache.simple;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCache;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheConfiguration;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheFactory;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstanceFactory;
import org.jboss.as.server.ServerEnvironment;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Configures a service that provides a simple stateful session bean cache factory.
 *
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class SimpleStatefulSessionBeanCacheFactoryServiceInstallerFactory<K, V extends StatefulSessionBeanInstance<K>> implements Function<StatefulComponentDescription, ServiceInstaller> {

    @Override
    public ServiceInstaller apply(StatefulComponentDescription description) {
        ServiceDependency<ServerEnvironment> environment = ServiceDependency.on(ServerEnvironment.SERVICE_DESCRIPTOR);
        StatefulSessionBeanCacheFactory<K, V> factory = new StatefulSessionBeanCacheFactory<>() {
            @Override
            public StatefulSessionBeanCache<K, V> createStatefulBeanCache(StatefulSessionBeanCacheConfiguration<K, V> configuration) {
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
                    public Optional<Duration> getMaxIdle() {
                        return configuration.getMaxIdle();
                    }

                    @Override
                    public ServerEnvironment getEnvironment() {
                        return environment.get();
                    }

                    @Override
                    public String getComponentName() {
                        return configuration.getComponentName();
                    }
                });
            }
        };
        return ServiceInstaller.builder(factory)
                .provides(description.getCacheFactoryServiceName())
                .requires(environment)
                .build();
    }
}
