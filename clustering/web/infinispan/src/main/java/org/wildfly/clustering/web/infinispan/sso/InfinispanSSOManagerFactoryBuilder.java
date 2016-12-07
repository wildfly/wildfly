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
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

public class InfinispanSSOManagerFactoryBuilder<A, D> implements CapabilityServiceBuilder<SSOManagerFactory<A, D, TransactionBatch>>, Value<SSOManagerFactory<A, D, TransactionBatch>>, InfinispanSSOManagerFactoryConfiguration {

    public static final String DEFAULT_CACHE_CONTAINER = "web";

    private final String host;
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();

    private final CapabilityServiceBuilder<?> configurationBuilder;
    private final CapabilityServiceBuilder<?> cacheBuilder;

    private volatile ValueDependency<KeyAffinityServiceFactory> affinityFactory;

    public InfinispanSSOManagerFactoryBuilder(String host) {
        this.host = host;

        this.configurationBuilder = new TemplateConfigurationBuilder(ServiceName.parse(InfinispanCacheRequirement.CONFIGURATION.resolve(DEFAULT_CACHE_CONTAINER, host)), DEFAULT_CACHE_CONTAINER, host, null);
        this.cacheBuilder = new CacheBuilder<>(ServiceName.parse(InfinispanCacheRequirement.CACHE.resolve(DEFAULT_CACHE_CONTAINER, host)), DEFAULT_CACHE_CONTAINER, host);
    }

    @Override
    public ServiceName getServiceName() {
        return ServiceName.JBOSS.append("clustering", "sso", this.host);
    }

    @Override
    public Builder<SSOManagerFactory<A, D, TransactionBatch>> configure(CapabilityServiceSupport support) {
        this.configurationBuilder.configure(support);
        this.cacheBuilder.configure(support);

        this.affinityFactory = new InjectedValueDependency<>(InfinispanRequirement.KEY_AFFINITY_FACTORY.getServiceName(support, DEFAULT_CACHE_CONTAINER), KeyAffinityServiceFactory.class);
        return this;
    }

    @Override
    public ServiceBuilder<SSOManagerFactory<A, D, TransactionBatch>> build(ServiceTarget target) {
        this.configurationBuilder.build(target).install();
        this.cacheBuilder.build(target).install();

        ServiceBuilder<SSOManagerFactory<A, D, TransactionBatch>> builder = target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(this.cacheBuilder.getServiceName(), Cache.class, this.cache)
        ;
        return this.affinityFactory.register(builder);
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
}
