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

import org.infinispan.Cache;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

public class InfinispanSSOManagerFactoryBuilder<A, D> implements Builder<SSOManagerFactory<A, D, TransactionBatch>>, Value<SSOManagerFactory<A, D, TransactionBatch>>, InfinispanSSOManagerFactoryConfiguration {

    public static final String DEFAULT_CACHE_CONTAINER = "web";

    private final CapabilityServiceSupport support;
    private final String host;
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();
    private final InjectedValue<KeyAffinityServiceFactory> affinityFactory = new InjectedValue<>();
    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();

    public InfinispanSSOManagerFactoryBuilder(CapabilityServiceSupport support, String host) {
        this.support = support;
        this.host = host;
    }

    @Override
    public ServiceName getServiceName() {
        return ServiceName.JBOSS.append("clustering", "sso", this.host);
    }

    @Override
    public ServiceBuilder<SSOManagerFactory<A, D, TransactionBatch>> build(ServiceTarget target) {
        String containerName = DEFAULT_CACHE_CONTAINER;
        String templateCacheName = null;
        String cacheName = this.host;

        new TemplateConfigurationBuilder(InfinispanCacheRequirement.CONFIGURATION.getServiceName(this.support, containerName, cacheName), containerName, cacheName, templateCacheName).configure(this.support).build(target).install();

        new CacheBuilder<>(InfinispanCacheRequirement.CACHE.getServiceName(this.support, containerName, cacheName), containerName, cacheName).configure(this.support).build(target).install();

        return target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(InfinispanCacheRequirement.CACHE.getServiceName(this.support, containerName, cacheName), Cache.class, this.cache)
                .addDependency(InfinispanRequirement.KEY_AFFINITY_FACTORY.getServiceName(this.support, containerName), KeyAffinityServiceFactory.class, this.affinityFactory)
                .addDependency(ServiceName.JBOSS.append("as", "service-module-loader"), ModuleLoader.class, this.loader)
        ;
    }

    @Override
    public SSOManagerFactory<A, D, TransactionBatch> getValue() {
        return new InfinispanSSOManagerFactory<>(this);
    }

    @Override
    public <K, V> Cache<K, V> getCache() {
        return this.cache.getValue();
    }

    @Override
    public KeyAffinityServiceFactory getKeyAffinityServiceFactory() {
        return this.affinityFactory.getValue();
    }

    @Override
    public ModuleLoader getModuleLoader() {
        return this.loader.getValue();
    }
}
