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

import java.io.IOException;
import java.util.Map;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.CoreGroupCommunicationServiceService;
import org.jboss.as.clustering.infinispan.atomic.AtomicMapCache;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.RetryingCacheInvoker;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManagerService;
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
    public static final String DEFAULT_JVM_ROUTE_CACHE = "registry";
    public static final Short SCOPE_ID = Short.valueOf((short) 222);

    private SessionAttributeStorageFactory storageFactory = new SessionAttributeStorageFactoryImpl();
    private CacheInvoker invoker = new RetryingCacheInvoker(10, 100);
    private SessionAttributeMarshallerFactory marshallerFactory = new SessionAttributeMarshallerFactoryImpl();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> sessionCache = new InjectedValue<Cache>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> jvmRouteCache = new InjectedValue<Cache>();
    private final InjectedValue<SharedLocalYieldingClusterLockManager> lockManager = new InjectedValue<SharedLocalYieldingClusterLockManager>();

    @Override
    public <T extends OutgoingDistributableSessionData> org.jboss.as.clustering.web.DistributedCacheManager<T> getDistributedCacheManager(LocalDistributableSessionManager manager) throws ClusteringNotSupportedException {
        @SuppressWarnings("unchecked")
        AdvancedCache<SessionKeyImpl, Map<Object, Object>> sessionCache = this.sessionCache.getValue().getAdvancedCache().with(this.getClass().getClassLoader());
        if (!sessionCache.getCacheConfiguration().invocationBatching().enabled()) {
            throw new ClusteringNotSupportedException(MESSAGES.failedToConfigureWebApp(sessionCache.getCacheManager().getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName(), sessionCache.getName()));
        }
        @SuppressWarnings("unchecked")
        Cache<Address, String> jvmRouteCache = this.sessionCache.getValue();
        BatchingManager batchingManager = new TransactionBatchingManager(sessionCache.getTransactionManager());
        SessionAttributeStorage<T> storage = this.storageFactory.createStorage(manager.getReplicationConfig().getReplicationGranularity(), this.marshallerFactory.createMarshaller(manager));

        return new DistributedCacheManager<T, SessionKeyImpl>(manager, new AtomicMapCache<SessionKeyImpl, Object, Object>(sessionCache), jvmRouteCache, this.lockManager.getValue(), storage, batchingManager, new SessionKeyFactoryImpl(manager), this.invoker);
    }

    @Override
    public boolean addDependencies(ServiceTarget target, ServiceBuilder<?> builder, JBossWebMetaData metaData) {
        ServiceName baseServiceName = EmbeddedCacheManagerService.getServiceName(null);
        ReplicationConfig config = metaData.getReplicationConfig();
        String cacheName = (config != null) ? config.getCacheName() : null;
        ServiceName serviceName = ServiceName.parse((cacheName != null) ? cacheName : DEFAULT_CACHE_CONTAINER);
        if (!baseServiceName.isParentOf(serviceName)) {
            serviceName = baseServiceName.append(serviceName);
        }
        if (serviceName.length() < 4) {
            serviceName = serviceName.append(CacheContainer.DEFAULT_CACHE_NAME);
        }
        if (CurrentServiceContainer.getServiceContainer().getService(serviceName) == null) {
            return false;
        }
        String container = serviceName.getParent().getSimpleName();
        new CoreGroupCommunicationServiceService(SCOPE_ID).build(target, container).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        new SharedLocalYieldingClusterLockManagerService(container).build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        builder.addDependency(serviceName, Cache.class, this.sessionCache);
        builder.addDependency(CacheService.getServiceName(container, DEFAULT_JVM_ROUTE_CACHE), Cache.class, this.jvmRouteCache);
        builder.addDependency(SharedLocalYieldingClusterLockManagerService.getServiceName(container), SharedLocalYieldingClusterLockManager.class, this.lockManager);
        return true;
    }

    @SuppressWarnings("rawtypes")
    public Injector<Cache> getSessionCacheInjector() {
        return this.sessionCache;
    }

    @SuppressWarnings("rawtypes")
    public Injector<Cache> getJvmRouteCacheInjector() {
        return this.jvmRouteCache;
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

    private static class SessionKeyFactoryImpl implements SessionKeyFactory<SessionKeyImpl> {
        private final LocalDistributableSessionManager manager;

        public SessionKeyFactoryImpl(LocalDistributableSessionManager manager) {
            this.manager = manager;
        }

        /**
         * {@inheritDoc}
         * @see org.jboss.as.clustering.web.infinispan.SessionKeyFactory#createKey(java.lang.String)
         */
        @Override
        public SessionKeyImpl createKey(String sessionId) {
            return new SessionKeyImpl(this.manager.getName(), sessionId);
        }

        /**
         * {@inheritDoc}
         * @see org.jboss.as.clustering.web.infinispan.SessionKeyFactory#ours(org.jboss.as.clustering.web.infinispan.SessionKey)
         */
        @Override
        public boolean ours(SessionKeyImpl key) {
            return this.manager.getName().equals(key.application);
        }
    }

    public static class SessionKeyImpl implements SessionKey {
        private static final long serialVersionUID = 398539176014850559L;

        private final String application;
        private final String sessionId;
        private transient int hashCode;

        public SessionKeyImpl(String application, String sessionId) {
            this.application = application;
            this.sessionId = sessionId;
            this.computeHashCode();
        }

        /**
         * {@inheritDoc}
         * @see org.jboss.as.clustering.web.infinispan.SessionKey#getSessionId()
         */
        @Override
        public String getSessionId() {
            return this.sessionId;
        }

        @Override
        public boolean equals(Object object) {
            if ((object == null) || !(object instanceof SessionKeyImpl)) return false;

            SessionKeyImpl key = (SessionKeyImpl) object;

            return this.hashCode == key.hashCode && this.application.equals(key.application) && this.sessionId.equals(key.sessionId);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }

        private void computeHashCode() {
            this.hashCode = this.application.hashCode() ^ this.sessionId.hashCode();
        }

        @Override
        public String toString() {
            return String.format("%s/%s", this.application, this.sessionId);
        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();

            this.computeHashCode();
        }
    }
}
