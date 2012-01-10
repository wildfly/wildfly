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

import java.io.File;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Properties;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.AbstractLoaderConfiguration;
import org.infinispan.configuration.cache.AbstractLoaderConfigurationBuilder;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.FileCacheStoreConfiguration;
import org.infinispan.configuration.cache.LoaderConfiguration;
import org.infinispan.configuration.cache.LoadersConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.TypedProperties;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.as.clustering.CoreGroupCommunicationServiceService;
import org.jboss.as.clustering.HashableMarshalledValueFactory;
import org.jboss.as.clustering.MarshalledValue;
import org.jboss.as.clustering.MarshalledValueFactory;
import org.jboss.as.clustering.MarshallingContext;
import org.jboss.as.clustering.SimpleMarshalledValueFactory;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.RetryingCacheInvoker;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManagerService;
import org.jboss.as.clustering.registry.Registry;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.PassivationManager;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreConfig;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreSource;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStore;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreSource;
import org.jboss.as.ejb3.cache.spi.SerializationGroup;
import org.jboss.as.ejb3.cache.spi.SerializationGroupMember;
import org.jboss.as.ejb3.cache.spi.impl.AbstractBackingCacheEntryStoreSource;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
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
    public static final short SCOPE_ID = 223;

    private String cacheName = DEFAULT_BACKING_CACHE;
    private int maxSize = ClusteredBackingCacheEntryStoreConfig.DEFAULT_MAX_SIZE;
    private boolean passivateEventsOnReplicate = DEFAULT_PASSIVATE_EVENTS_ON_REPLICATE;

    private final MarshallerFactory factory = Marshalling.getMarshallerFactory("river", MarshallerFactory.class.getClassLoader());
    private CacheInvoker invoker = new RetryingCacheInvoker(0, 0);
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> groupCache = new InjectedValue<Cache>();
    private final InjectedValue<SharedLocalYieldingClusterLockManager> lockManager = new InjectedValue<SharedLocalYieldingClusterLockManager>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Registry> registry = new InjectedValue<Registry>();

    @Override
    public void addDependencies(ServiceTarget target, ServiceBuilder<?> builder) {
        ServiceName baseServiceName = EmbeddedCacheManagerService.getServiceName(null);
        ServiceName serviceName = ServiceName.parse((this.cacheName != null) ? this.cacheName : DEFAULT_BACKING_CACHE);
        if (!baseServiceName.isParentOf(serviceName)) {
            serviceName = baseServiceName.append(serviceName);
        }
        if (serviceName.length() < 4) {
            serviceName = serviceName.append(CacheContainer.DEFAULT_CACHE_NAME);
        }
        String container = serviceName.getParent().getSimpleName();
        // install the GroupCommunicationService
        new CoreGroupCommunicationServiceService(SCOPE_ID).build(target, container).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        new SharedLocalYieldingClusterLockManagerService(container).build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        builder.addDependency(SharedLocalYieldingClusterLockManagerService.getServiceName(container), SharedLocalYieldingClusterLockManager.class, this.lockManager);
        builder.addDependency(EJBRemoteConnectorService.EJB_REMOTE_CONNECTOR_CLIENT_MAPPINGS_REGISTRY_SERVICE, Registry.class, this.registry);
    }

    @Override
    public <E extends SerializationGroup<K, V, G>> BackingCacheEntryStore<G, Cacheable<G>, E> createGroupIntegratedObjectStore(PassivationManager<G, E> passivationManager, StatefulTimeoutInfo timeout) {
        @SuppressWarnings("unchecked")
        Cache<MarshalledValue<G, MarshallingContext>, MarshalledValue<E, MarshallingContext>> cache = this.groupCache.getValue().getAdvancedCache().with(this.getClass().getClassLoader());
        MarshallingContext context = new MarshallingContext(this.factory, passivationManager.getMarshallingConfiguration());
        MarshalledValueFactory<MarshallingContext> keyFactory = new HashableMarshalledValueFactory(context);
        MarshalledValueFactory<MarshallingContext> valueFactory = new SimpleMarshalledValueFactory(context);
        LockKeyFactory<G, MarshallingContext> lockKeyFactory = new LockKeyFactory<G, MarshallingContext>() {
            @Override
            public Serializable createLockKey(MarshalledValue<G, MarshallingContext> key) {
                return key;
            }
        };
        @SuppressWarnings("unchecked")
        Registry<String, ?> registry = this.registry.getValue();
        return new InfinispanBackingCacheEntryStore<G, Cacheable<G>, E, MarshallingContext>(cache, this.invoker, null, timeout, this, false, keyFactory, valueFactory, context, this.lockManager.getValue(), lockKeyFactory, registry);
    }

    @Override
    public <E extends SerializationGroupMember<K, V, G>> BackingCacheEntryStore<K, V, E> createIntegratedObjectStore(final String beanName, PassivationManager<K, E> passivationManager, StatefulTimeoutInfo timeout) {
        Cache<?, ?> groupCache = this.groupCache.getValue();
        Configuration groupCacheConfiguration = groupCache.getCacheConfiguration();
        EmbeddedCacheManager container = groupCache.getCacheManager();
        ConfigurationBuilder builder = new ConfigurationBuilder().read(groupCacheConfiguration);
        if (this.maxSize > 0) {
            builder.eviction().strategy(EvictionStrategy.LRU).maxEntries(this.maxSize);
        }
        // Clear out loaders configuration
        builder.loaders()
                .read(new ConfigurationBuilder().build().loaders())
                .passivation(groupCacheConfiguration.loaders().passivation())
                .preload(groupCacheConfiguration.loaders().preload())
                .shared(groupCacheConfiguration.loaders().shared())
        ;
        // Our cache needs a unique passivation location
        for (AbstractLoaderConfiguration loader: groupCacheConfiguration.loaders().cacheLoaders()) {
            this.addCacheLoader(builder.loaders(), loader, beanName)
                .async().read(loader.async())
                .singletonStore().read(loader.singletonStore())
            ;
        }
        groupCache.getCacheManager().defineConfiguration(beanName, builder.build());
        Cache<MarshalledValue<K, MarshallingContext>, MarshalledValue<E, MarshallingContext>> cache = container.<MarshalledValue<K, MarshallingContext>, MarshalledValue<E, MarshallingContext>>getCache(beanName).getAdvancedCache().with(this.getClass().getClassLoader());
        MarshallingContext context = new MarshallingContext(this.factory, passivationManager.getMarshallingConfiguration());
        MarshalledValueFactory<MarshallingContext> keyFactory = new HashableMarshalledValueFactory(context);
        MarshalledValueFactory<MarshallingContext> valueFactory = new SimpleMarshalledValueFactory(context);
        LockKeyFactory<K, MarshallingContext> lockKeyFactory = new LockKeyFactory<K, MarshallingContext>() {
            @Override
            public Serializable createLockKey(MarshalledValue<K, MarshallingContext> key) {
                return new AbstractMap.SimpleImmutableEntry<MarshalledValue<K, MarshallingContext>, String>(key, beanName);
            }
        };
        @SuppressWarnings("unchecked")
        Registry<String, ?> registry = this.registry.getValue();
        return new InfinispanBackingCacheEntryStore<K, V, E, MarshallingContext>(cache, this.invoker, this.passivateEventsOnReplicate ? passivationManager : null, timeout, this, true, keyFactory, valueFactory, context, this.lockManager.getValue(), lockKeyFactory, registry);
    }

    private AbstractLoaderConfigurationBuilder<?> addCacheLoader(LoadersConfigurationBuilder loadersBuilder, AbstractLoaderConfiguration config, String beanName) {
        if (config instanceof LoaderConfiguration) {
            LoaderConfiguration loader = (LoaderConfiguration) config;
            Properties properties = new TypedProperties(loader.properties());
            String location = loader.properties().getProperty("location");
            if (location != null) {
                properties.setProperty("location", this.createBeanLocation(location, beanName));
            }
            return loadersBuilder.addCacheLoader()
                    .read(loader)
                    .withProperties(properties)
            ;
        } else if (config instanceof FileCacheStoreConfiguration) {
            FileCacheStoreConfiguration store = (FileCacheStoreConfiguration) config;
            return loadersBuilder.addFileCacheStore()
                    .read(store)
                    .location(this.createBeanLocation(store.location(), beanName))
            ;
        }
        throw new IllegalStateException(String.format("Unsupported cache loader: %s", config.getClass().getName()));
    }

    private String createBeanLocation(String location, String beanName) {
        return location + File.separatorChar + beanName;
    }

    @Override
    public String getBackingCache() {
        return this.cacheName;
    }

    @Override
    public void setBackingCache(String cacheName) {
        this.cacheName = cacheName;
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
}
