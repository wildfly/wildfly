/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web.sso.infinispan;

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
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.service.InfinispanRequirement;
import org.wildfly.clustering.infinispan.service.TemplateConfigurationServiceConfigurator;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.infinispan.sso.InfinispanSSOManagerFactory;
import org.wildfly.clustering.web.infinispan.sso.InfinispanSSOManagerFactoryConfiguration;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

public class InfinispanSSOManagerFactoryServiceConfigurator<A, D, S> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, InfinispanSSOManagerFactoryConfiguration {

    private final String name;
    private final String containerName;
    private final String cacheName;

    private volatile ServiceConfigurator configurationConfigurator;
    private volatile ServiceConfigurator cacheConfigurator;
    private volatile SupplierDependency<KeyAffinityServiceFactory> affinityFactory;

    @SuppressWarnings("rawtypes")
    private volatile Supplier<Cache> cache;

    public InfinispanSSOManagerFactoryServiceConfigurator(InfinispanSSOManagementConfiguration config, String name) {
        super(ServiceName.JBOSS.append("clustering", "sso", name));

        this.name = name;
        this.containerName = config.getContainerName();
        this.cacheName = config.getCacheName();
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.configurationConfigurator = new TemplateConfigurationServiceConfigurator(InfinispanCacheRequirement.CONFIGURATION.getServiceName(support, this.containerName, this.name), this.containerName, this.name, this.cacheName).configure(support);
        this.cacheConfigurator = new CacheServiceConfigurator<>(InfinispanCacheRequirement.CACHE.getServiceName(support, this.containerName, this.name), this.containerName, this.name).configure(support);
        this.affinityFactory = new ServiceSupplierDependency<>(InfinispanRequirement.KEY_AFFINITY_FACTORY.getServiceName(support, this.containerName));
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
