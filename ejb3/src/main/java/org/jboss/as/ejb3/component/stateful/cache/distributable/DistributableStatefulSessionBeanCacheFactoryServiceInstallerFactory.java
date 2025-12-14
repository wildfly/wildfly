/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache.distributable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

import org.jboss.as.controller.management.Capabilities;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCache;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheConfiguration;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheFactory;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstanceFactory;
import org.jboss.as.ejb3.component.stateful.cache.SuspendableStatefulSessionBeanCache;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.wildfly.clustering.ejb.bean.BeanManager;
import org.wildfly.clustering.ejb.bean.BeanManagerConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagerFactory;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Configures a service providing a distributable stateful session bean cache factory.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class DistributableStatefulSessionBeanCacheFactoryServiceInstallerFactory<K, V extends StatefulSessionBeanInstance<K>> implements BiFunction<StatefulComponentDescription, ServiceDependency<BeanManagerFactory<K, V>>, ServiceInstaller> {

    @Override
    public ServiceInstaller apply(StatefulComponentDescription description, ServiceDependency<BeanManagerFactory<K, V>> managerFactory) {
        ServiceDependency<SuspendableActivityRegistry> activityRegistry = ServiceDependency.on(SuspendableActivityRegistry.SERVICE_DESCRIPTOR);
        ServiceDependency<Executor> executor = ServiceDependency.on(Capabilities.MANAGEMENT_EXECUTOR);
        StatefulSessionBeanCacheFactory<K, V> factory = new StatefulSessionBeanCacheFactory<>() {
            @Override
            public StatefulSessionBeanCache<K, V> createStatefulBeanCache(StatefulSessionBeanCacheConfiguration<K, V> configuration) {
                Consumer<V> timeoutListener = StatefulSessionBeanInstance::removed;
                BeanManager<K, V> manager = managerFactory.get().createBeanManager(new BeanManagerConfiguration<>() {
                    @Override
                    public Supplier<K> getIdentifierFactory() {
                        return configuration.getIdentifierFactory();
                    }

                    @Override
                    public String getBeanName() {
                        return configuration.getComponentName();
                    }

                    @Override
                    public Optional<Duration> getMaxIdle() {
                        return configuration.getMaxIdle();
                    }

                    @Override
                    public Consumer<V> getExpirationListener() {
                        return timeoutListener;
                    }
                });
                return new SuspendableStatefulSessionBeanCache<>(new DistributableStatefulSessionBeanCache<>(new DistributableStatefulSessionBeanCacheConfiguration<>() {
                    @Override
                    public StatefulSessionBeanInstanceFactory<V> getInstanceFactory() {
                        return configuration.getInstanceFactory();
                    }

                    @Override
                    public Supplier<K> getIdentifierFactory() {
                        return manager.getIdentifierFactory();
                    }

                    @Override
                    public BeanManager<K, V> getBeanManager() {
                        return manager;
                    }

                    @Override
                    public Optional<Duration> getMaxIdle() {
                        return configuration.getMaxIdle();
                    }

                    @Override
                    public String getComponentName() {
                        return configuration.getComponentName();
                    }
                }), activityRegistry.get(), executor.get());
            }
        };
        return ServiceInstaller.builder(factory)
                .provides(description.getCacheFactoryServiceName())
                .startWhen(StartWhen.REQUIRED)
                .requires(List.of(managerFactory, activityRegistry, executor))
                .build();
    }
}
