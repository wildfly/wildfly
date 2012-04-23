/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.clustering.ejb3.cache.backing.infinispan;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.concurrent.Executors;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.MarshalledValue;
import org.jboss.as.clustering.MarshalledValueFactory;
import org.jboss.as.clustering.MarshallingContext;
import org.jboss.as.clustering.SimpleMarshalledValueFactory;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.jboss.as.clustering.infinispan.affinity.LocalKeyAffinityServiceFactory;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.RetryingCacheInvoker;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager;
import org.jboss.as.clustering.lock.impl.SharedLocalYieldingClusterLockManagerService;
import org.jboss.as.clustering.registry.Registry;
import org.jboss.as.clustering.registry.RegistryService;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.IdentifierFactory;
import org.jboss.as.ejb3.cache.PassivationManager;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreConfig;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreSource;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreSourceService;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStore;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreSource;
import org.jboss.as.ejb3.cache.spi.SerializationGroup;
import org.jboss.as.ejb3.cache.spi.SerializationGroupMember;
import org.jboss.as.ejb3.cache.spi.impl.AbstractBackingCacheEntryStoreSource;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.ejb3.remote.EJBRemotingConnectorClientMappingsEntryProviderService;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * {@link BackingCacheEntryStoreSource} that provides instances of {@link InfinispanBackingCacheEntryStore}.
 *
 * @author Brian Stansberry
 */
public class InfinispanBackingCacheEntryStoreSource<K extends Serializable, V extends Cacheable<K>, G extends Serializable> extends AbstractBackingCacheEntryStoreSource<K, V, G> implements ClusteredBackingCacheEntryStoreSource<K, V, G> {

    private String cacheContainerName = DEFAULT_CACHE_CONTAINER;
    private String beanCacheName = DEFAULT_BEAN_CACHE;
    private String clientMappingsCacheName = DEFAULT_CLIENT_MAPPING_CACHE;
    private int maxSize = ClusteredBackingCacheEntryStoreConfig.DEFAULT_MAX_SIZE;
    private boolean passivateEventsOnReplicate = DEFAULT_PASSIVATE_EVENTS_ON_REPLICATE;
    private KeyAffinityServiceFactory affinityFactory = new LocalKeyAffinityServiceFactory(Executors.newSingleThreadExecutor(), 10);

    private final MarshallerFactory factory = Marshalling.getMarshallerFactory("river", MarshallerFactory.class.getClassLoader());
    private CacheInvoker invoker = new RetryingCacheInvoker(0, 0);
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> groupCache = new InjectedValue<Cache>();
    private final InjectedValue<SharedLocalYieldingClusterLockManager> lockManager = new InjectedValue<SharedLocalYieldingClusterLockManager>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Registry> registry = new InjectedValue<Registry>();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void addDependencies(ServiceTarget target, ServiceBuilder<?> builder) {
        ServiceName groupCacheServiceName = CacheService.getServiceName(this.cacheContainerName, this.beanCacheName);
        // AS7-3906 Ensure that the cache manager's rpc dispatcher starts before GroupCommunicationService's
        builder.addDependency(groupCacheServiceName, Cache.class, this.groupCache);
        builder.addDependency(SharedLocalYieldingClusterLockManagerService.getServiceName(this.cacheContainerName), SharedLocalYieldingClusterLockManager.class, this.lockManager);
        builder.addDependency(ClusteredBackingCacheEntryStoreSourceService.getClientMappingRegistryServiceName(this.cacheContainerName), Registry.class, this.registry);

        InjectedValue<Cache> cache = new InjectedValue<Cache>();
        InjectedValue<Registry.RegistryEntryProvider> provider = new InjectedValue<Registry.RegistryEntryProvider>();
        target.addService(ClusteredBackingCacheEntryStoreSourceService.getClientMappingRegistryServiceName(this.cacheContainerName), new RegistryService(cache, provider))
                .addDependency(EJBRemotingConnectorClientMappingsEntryProviderService.SERVICE_NAME, Registry.RegistryEntryProvider.class, provider)
                .addDependency(CacheService.getServiceName(this.cacheContainerName, this.clientMappingsCacheName), Cache.class, cache)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
        InjectedValue<EmbeddedCacheManager> container = new InjectedValue<EmbeddedCacheManager>();
        target.addService(ClusteredBackingCacheEntryStoreSourceService.getCacheContainerClusterNameServiceName(this.cacheContainerName), new ClusterNameService(container))
                .addDependency(EmbeddedCacheManagerService.getServiceName(this.cacheContainerName), EmbeddedCacheManager.class, container)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
    }

    @Override
    public <E extends SerializationGroup<K, V, G>> BackingCacheEntryStore<G, Cacheable<G>, E> createGroupIntegratedObjectStore(IdentifierFactory<G> identifierFactory, PassivationManager<G, E> passivationManager, StatefulTimeoutInfo timeout) {
        @SuppressWarnings("unchecked")
        Cache<G, MarshalledValue<E, MarshallingContext>> cache = this.groupCache.getValue();
        MarshallingContext context = new MarshallingContext(this.factory, passivationManager.getMarshallingConfiguration());
        MarshalledValueFactory<MarshallingContext> valueFactory = new SimpleMarshalledValueFactory(context);
        @SuppressWarnings("unchecked")
        Registry<String, ?> registry = this.registry.getValue();
        return new InfinispanBackingCacheEntryStore<G, Cacheable<G>, E, MarshallingContext>(cache, this.invoker, identifierFactory, this.affinityFactory, null, timeout, this, false, valueFactory, context, null, null, registry);
    }

    @Override
    public <E extends SerializationGroupMember<K, V, G>> BackingCacheEntryStore<K, V, E> createIntegratedObjectStore(final String beanName, IdentifierFactory<K> identifierFactory, PassivationManager<K, E> passivationManager, StatefulTimeoutInfo timeout) {
        Cache<?, ?> groupCache = this.groupCache.getValue();
        Configuration groupCacheConfiguration = groupCache.getCacheConfiguration();
        EmbeddedCacheManager container = groupCache.getCacheManager();
        ConfigurationBuilder builder = new ConfigurationBuilder().read(groupCacheConfiguration);
        if (this.maxSize > 0) {
            if (!groupCacheConfiguration.eviction().strategy().isEnabled()) {
                builder.eviction().strategy(EvictionStrategy.LRU);
            }
            builder.eviction().maxEntries(this.maxSize);
        }
        groupCache.getCacheManager().defineConfiguration(beanName, builder.build());
        Cache<K, MarshalledValue<E, MarshallingContext>> cache = container.<K, MarshalledValue<E, MarshallingContext>>getCache(beanName);
        MarshallingContext context = new MarshallingContext(this.factory, passivationManager.getMarshallingConfiguration());
        MarshalledValueFactory<MarshallingContext> valueFactory = new SimpleMarshalledValueFactory(context);
        LockKeyFactory<K> lockKeyFactory = new LockKeyFactory<K>() {
            @Override
            public Serializable createLockKey(K key) {
                return new AbstractMap.SimpleImmutableEntry<K, String>(key, beanName);
            }
        };
        @SuppressWarnings("unchecked")
        Registry<String, ?> registry = this.registry.getValue();
        return new InfinispanBackingCacheEntryStore<K, V, E, MarshallingContext>(cache, this.invoker, identifierFactory, this.affinityFactory, this.passivateEventsOnReplicate ? passivationManager : null, timeout, this, true, valueFactory, context, this.lockManager.getValue(), lockKeyFactory, registry);
    }

    public void setKeyAffinityServiceFactory(KeyAffinityServiceFactory affinityFactory) {
        this.affinityFactory = affinityFactory;
    }

    @Override
    public String getCacheContainer() {
        return this.cacheContainerName;
    }

    @Override
    public void setCacheContainer(String cacheContainerName) {
        this.cacheContainerName = cacheContainerName;
    }

    @Override
    public String getClientMappingCache() {
        return this.clientMappingsCacheName;
    }

    @Override
    public void setClientMappingCache(String cacheName) {
        this.clientMappingsCacheName = cacheName;
    }

    @Override
    public String getBeanCache() {
        return this.beanCacheName;
    }

    @Override
    public void setBeanCache(String cacheName) {
        this.beanCacheName = cacheName;
    }

    @Override
    public boolean isPassivateEventsOnReplicate() {
        return this.passivateEventsOnReplicate;
    }

    @Override
    public void setPassivateEventsOnReplicate(boolean passivateEventsOnReplicate) {
        this.passivateEventsOnReplicate = passivateEventsOnReplicate;
    }

    @Override
    public int getMaxSize() {
        return this.maxSize;
    }

    @Override
    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public void started() {
    }

    @Override
    public void stopped() {
    }
}
