/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web.infinispan;

import static org.jboss.as.clustering.web.infinispan.InfinispanWebMessages.MESSAGES;

import java.util.Map;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.CoreGroupCommunicationServiceService;
import org.jboss.as.clustering.infinispan.atomic.AtomicMapCache;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.RetryingCacheInvoker;
import org.jboss.as.clustering.infinispan.subsystem.CacheConfigurationService;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManagerService;
import org.jboss.as.clustering.registry.Registry;
import org.jboss.as.clustering.registry.RegistryService;
import org.jboss.as.clustering.web.BatchingManager;
import org.jboss.as.clustering.web.ClusteringNotSupportedException;
import org.jboss.as.clustering.web.LocalDistributableSessionManager;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.clustering.web.SessionAttributeMarshallerFactory;
import org.jboss.as.clustering.web.impl.SessionAttributeMarshallerFactoryImpl;
import org.jboss.as.clustering.web.impl.TransactionBatchingManager;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * Factory for creating an Infinispan-backed distributed cache manager.
 *
 * @author Paul Ferraro
 */
public class DistributedCacheManagerFactory implements org.jboss.as.clustering.web.DistributedCacheManagerFactory {
    public static final String DEFAULT_CACHE_CONTAINER = "web";
    public static final Short SCOPE_ID = Short.valueOf((short) 222);

    private final JvmRouteRegistryEntryProvider registryEntryProvider = new JvmRouteRegistryEntryProvider();
    private SessionAttributeStorageFactory storageFactory = new SessionAttributeStorageFactoryImpl();
    private CacheInvoker invoker = new RetryingCacheInvoker(10, 100);
    private SessionAttributeMarshallerFactory marshallerFactory = new SessionAttributeMarshallerFactoryImpl();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Registry> registry = new InjectedValue<Registry>();
    private final InjectedValue<EmbeddedCacheManager> container = new InjectedValue<EmbeddedCacheManager>();
    private final InjectedValue<Configuration> config = new InjectedValue<Configuration>();
    private final InjectedValue<SharedLocalYieldingClusterLockManager> lockManager = new InjectedValue<SharedLocalYieldingClusterLockManager>();

    @Override
    public <T extends OutgoingDistributableSessionData> org.jboss.as.clustering.web.DistributedCacheManager<T> getDistributedCacheManager(LocalDistributableSessionManager manager) throws ClusteringNotSupportedException {
        this.registryEntryProvider.setManager(manager);
        EmbeddedCacheManager container = this.container.getValue();
        @SuppressWarnings("unchecked")
        Registry<String, Void> jvmRouteRegistry = this.registry.getValue();
        Configuration config = this.config.getValue();

        if (!config.invocationBatching().enabled()) {
            ServiceName cacheServiceName = this.getCacheServiceName(manager.getReplicationConfig());
            throw new ClusteringNotSupportedException(MESSAGES.failedToConfigureWebApp(cacheServiceName.getParent().getSimpleName(), cacheServiceName.getSimpleName()));
        }

        String name = manager.getName();
        ConfigurationBuilder builder = new ConfigurationBuilder().read(config);
        builder.storeAsBinary().enable().storeKeysAsBinary(false).storeValuesAsBinary(true);
        container.defineConfiguration(name, builder.build());

        AdvancedCache<String, Map<Object, Object>> sessionCache = container.<String, Map<Object, Object>>getCache(name).getAdvancedCache().with(this.getClass().getClassLoader());
        BatchingManager batchingManager = new TransactionBatchingManager(sessionCache.getTransactionManager());
        SessionAttributeStorage<T> storage = this.storageFactory.createStorage(manager.getReplicationConfig().getReplicationGranularity(), this.marshallerFactory.createMarshaller(manager));

        return new DistributedCacheManager<T>(manager, new AtomicMapCache<String, Object, Object>(sessionCache), jvmRouteRegistry, this.lockManager.getOptionalValue(), storage, batchingManager, this.invoker);
    }

    @Override
    public boolean addDependencies(ServiceTarget target, ServiceBuilder<?> builder, JBossWebMetaData metaData) {
        ServiceName cacheServiceName = this.getCacheServiceName(metaData.getReplicationConfig());
        ServiceContainer serviceContainer = CurrentServiceContainer.getServiceContainer();
        if (serviceContainer.getService(cacheServiceName) == null) {
            return false;
        }
        ServiceName containerServiceName = cacheServiceName.getParent();
        String container = containerServiceName.getSimpleName();
        ServiceName lockManagerServiceName = SharedLocalYieldingClusterLockManagerService.getServiceName(container);
        ServiceName registryServiceName = cacheServiceName.append("registry");
        synchronized (this) {
            if (serviceContainer.getService(lockManagerServiceName) == null) {
                new CoreGroupCommunicationServiceService(SCOPE_ID).build(target, container).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
                new SharedLocalYieldingClusterLockManagerService(container).build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
            }
            if (serviceContainer.getService(registryServiceName) == null) {
                new RegistryService<String, Void>(this.registryEntryProvider).build(target, registryServiceName, cacheServiceName).install();
            }
        }
        builder.addDependency(containerServiceName, EmbeddedCacheManager.class, this.container);
        builder.addDependency(CacheConfigurationService.getServiceName(container, cacheServiceName.getSimpleName()), Configuration.class, this.config);
        builder.addDependency(registryServiceName, Registry.class, this.registry);
        builder.addDependency(SharedLocalYieldingClusterLockManagerService.getServiceName(container), SharedLocalYieldingClusterLockManager.class, this.lockManager);
        return true;
    }

    private ServiceName getCacheServiceName(ReplicationConfig config) {
        ServiceName baseServiceName = EmbeddedCacheManagerService.getServiceName(null);
        String cacheName = (config != null) ? config.getCacheName() : null;
        ServiceName serviceName = ServiceName.parse((cacheName != null) ? cacheName : DEFAULT_CACHE_CONTAINER);
        if (!baseServiceName.isParentOf(serviceName)) {
            serviceName = baseServiceName.append(serviceName);
        }
        return (serviceName.length() < 4) ? serviceName.append(CacheContainer.DEFAULT_CACHE_NAME) : serviceName;
    }

    public Injector<EmbeddedCacheManager> getCacheContainerInjector() {
        return this.container;
    }

    public Injector<Configuration> getCacheConfigurationInjector() {
        return this.config;
    }

    @SuppressWarnings("rawtypes")
    public Injector<Registry> getRegistryInjector() {
        return this.registry;
    }

    public Injector<SharedLocalYieldingClusterLockManager> getLockManagerInjector() {
        return this.lockManager;
    }

    public void setSessionAttributeMarshallerFactory(SessionAttributeMarshallerFactory marshallerFactory) {
        this.marshallerFactory = marshallerFactory;
    }

    public void setSessionAttributeStorageFactory(SessionAttributeStorageFactory storageFactory) {
        this.storageFactory = storageFactory;
    }

    public void setCacheInvoker(CacheInvoker invoker) {
        this.invoker = invoker;
    }

    class JvmRouteRegistryEntryProvider implements Registry.RegistryEntryProvider<String, Void> {
        private volatile LocalDistributableSessionManager manager;

        void setManager(LocalDistributableSessionManager manager) {
            this.manager = manager;
        }

        @Override
        public String getKey() {
            return (this.manager != null) ? this.manager.getJvmRoute() : null;
        }

        @Override
        public Void getValue() {
            return null;
        }
    }
}
