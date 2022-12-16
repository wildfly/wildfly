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

package org.jboss.as.ejb3.component.stateful.cache.distributable;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCache;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheConfiguration;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheFactory;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstanceFactory;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ejb.bean.BeanExpirationConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManager;
import org.wildfly.clustering.ejb.bean.BeanManagerConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagerFactory;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Configures a service providing a distributable stateful session bean cache factory.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class DistributableStatefulSessionBeanCacheFactoryServiceConfigurator<K, V extends StatefulSessionBeanInstance<K>> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, StatefulSessionBeanCacheFactory<K, V> {

    private final CapabilityServiceConfigurator configurator;
    private final SupplierDependency<BeanManagerFactory<K, V, Batch>> factory;

    public DistributableStatefulSessionBeanCacheFactoryServiceConfigurator(ServiceName name, CapabilityServiceConfigurator configurator) {
        super(name);
        this.configurator = configurator;
        this.factory = new ServiceSupplierDependency<>(configurator);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.configurator.configure(support);
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        this.configurator.build(target).install();
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<StatefulSessionBeanCacheFactory<K, V>> factory = new CompositeDependency(this.factory).register(builder).provides(name);
        Service service = Service.newInstance(factory, this);
        return builder.setInstance(service);
    }

    @Override
    public StatefulSessionBeanCache<K, V> createStatefulBeanCache(StatefulSessionBeanCacheConfiguration<K, V> configuration) {
        Duration timeout = configuration.getTimeout();
        Consumer<V> timeoutListener = StatefulSessionBeanInstance::removed;
        BeanExpirationConfiguration<K, V> expiration = (timeout != null) ? new BeanExpirationConfiguration<>() {
            @Override
            public Duration getTimeout() {
                return timeout;
            }

            @Override
            public Consumer<V> getExpirationListener() {
                return timeoutListener;
            }
        } : null;
        BeanManager<K, V, Batch> manager = this.factory.get().createBeanManager(new BeanManagerConfiguration<>() {
            @Override
            public Supplier<K> getIdentifierFactory() {
                return configuration.getIdentifierFactory();
            }

            @Override
            public String getBeanName() {
                return configuration.getComponentName();
            }

            @Override
            public BeanExpirationConfiguration<K, V> getExpiration() {
                return expiration;
            }
        });
        return new DistributableStatefulSessionBeanCache<>(new DistributableStatefulSessionBeanCacheConfiguration<>() {
            @Override
            public StatefulSessionBeanInstanceFactory<V> getInstanceFactory() {
                return configuration.getInstanceFactory();
            }

            @Override
            public Supplier<K> getIdentifierFactory() {
                return manager.getIdentifierFactory();
            }

            @Override
            public BeanManager<K, V, Batch> getBeanManager() {
                return manager;
            }

            @Override
            public Duration getTimeout() {
                return timeout;
            }

            @Override
            public String getComponentName() {
                return configuration.getComponentName();
            }
        });
    }
}
