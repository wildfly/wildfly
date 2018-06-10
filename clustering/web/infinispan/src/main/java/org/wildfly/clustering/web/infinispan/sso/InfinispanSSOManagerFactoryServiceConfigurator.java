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
package org.wildfly.clustering.web.infinispan.sso;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationServiceConfigurator;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

public class InfinispanSSOManagerFactoryServiceConfigurator<A, D, S> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, InfinispanSSOManagerFactoryConfiguration {

    public static final String DEFAULT_CACHE_CONTAINER = "web";

    private final CapabilityServiceConfigurator configurationConfigurator;
    private final CapabilityServiceConfigurator cacheConfigurator;

    private volatile SupplierDependency<KeyAffinityServiceFactory> affinityFactory;
    @SuppressWarnings("rawtypes")
    private volatile Supplier<Cache> cache;

    public InfinispanSSOManagerFactoryServiceConfigurator(String name) {
        super(ServiceName.JBOSS.append("clustering", "sso", name));

        this.configurationConfigurator = new TemplateConfigurationServiceConfigurator(ServiceName.parse(InfinispanCacheRequirement.CONFIGURATION.resolve(DEFAULT_CACHE_CONTAINER, name)), DEFAULT_CACHE_CONTAINER, name, null);
        this.cacheConfigurator = new CacheServiceConfigurator<>(ServiceName.parse(InfinispanCacheRequirement.CACHE.resolve(DEFAULT_CACHE_CONTAINER, name)), DEFAULT_CACHE_CONTAINER, name);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.configurationConfigurator.configure(support);
        this.cacheConfigurator.configure(support);

        this.affinityFactory = new ServiceSupplierDependency<>(InfinispanRequirement.KEY_AFFINITY_FACTORY.getServiceName(support, DEFAULT_CACHE_CONTAINER));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        this.configurationConfigurator.build(target).install();
        this.cacheConfigurator.build(target).install();

        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SSOManagerFactory<A, D, S, TransactionBatch>> factory = this.affinityFactory.register(builder).provides(this.getServiceName());
        this.cache = builder.requires(this.cacheConfigurator.getServiceName());
        Service service = Service.newInstance(factory, new InfinispanSSOManagerFactory<>(this));
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public <K, V> Cache<K, V> getCache() {
        return this.cache.get();
    }

    @Override
    public KeyAffinityServiceFactory getKeyAffinityServiceFactory() {
        return this.affinityFactory.get();
    }
}
