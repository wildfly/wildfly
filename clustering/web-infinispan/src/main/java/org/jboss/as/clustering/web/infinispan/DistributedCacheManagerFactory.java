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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.infinispan.AdvancedCache;
import org.infinispan.manager.CacheContainer;
import org.jboss.as.clustering.infinispan.atomic.AtomicMapCache;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.RetryingCacheInvoker;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager;
import org.jboss.as.clustering.web.BatchingManager;
import org.jboss.as.clustering.web.ClusteringNotSupportedException;
import org.jboss.as.clustering.web.LocalDistributableSessionManager;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.clustering.web.SessionAttributeMarshallerFactory;
import org.jboss.as.clustering.web.impl.SessionAttributeMarshallerFactoryImpl;
import org.jboss.as.clustering.web.impl.TransactionBatchingManager;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Factory for creating an Infinispan-backed distributed cache manager.
 *
 * @author Paul Ferraro
 */
public class DistributedCacheManagerFactory implements org.jboss.as.clustering.web.DistributedCacheManagerFactory {
    public static final String DEFAULT_CACHE_CONTAINER = "web";

    private ServiceNameProvider sessionCacheServiceNameProvider = new ServiceNameProvider() {
        @Override
        public ServiceName getServiceName(ReplicationConfig config) {
            ServiceName baseServiceName = EmbeddedCacheManagerService.getServiceName(null);
            String cacheName = config.getCacheName();
            ServiceName serviceName = ServiceName.parse((cacheName != null) ? cacheName : DEFAULT_CACHE_CONTAINER);
            if (!baseServiceName.isParentOf(serviceName)) {
                serviceName = baseServiceName.append(serviceName);
            }
            return (serviceName.length() < 4) ? serviceName.append(CacheContainer.DEFAULT_CACHE_NAME) : serviceName;
        }
    };
    private CacheSource sessionCacheSource = new SessionCacheSource(this.sessionCacheServiceNameProvider);
    private ServiceNameProvider jvmRouteCacheServiceNameProvider = new ServiceNameProvider() {
        @Override
        public ServiceName getServiceName(ReplicationConfig config) {
            return EmbeddedCacheManagerService.getServiceName(null);
        }
    };
    private CacheSource jvmRouteCacheSource = new JvmRouteCacheSource(this.jvmRouteCacheServiceNameProvider);
    private LockManagerSource lockManagerSource = new DefaultLockManagerSource();
    private SessionAttributeStorageFactory storageFactory = new SessionAttributeStorageFactoryImpl();
    private CacheInvoker invoker = new RetryingCacheInvoker(10, 100);
    private SessionAttributeMarshallerFactory marshallerFactory = new SessionAttributeMarshallerFactoryImpl();

    @Override
    public <T extends OutgoingDistributableSessionData> org.jboss.as.clustering.web.DistributedCacheManager<T> getDistributedCacheManager(ServiceRegistry registry, LocalDistributableSessionManager manager) throws ClusteringNotSupportedException {
        AdvancedCache<SessionKeyImpl, Map<Object, Object>> sessionCache = this.sessionCacheSource.<SessionKeyImpl, Map<Object, Object>>getCache(registry, manager).getAdvancedCache().with(this.getClass().getClassLoader());
        if (!sessionCache.getConfiguration().isInvocationBatchingEnabled()) {
            throw new ClusteringNotSupportedException(String.format("Failed to configure web application for <distributable/> sessions.  %s.%s cache requires batching=\"true\".", sessionCache.getCacheManager().getGlobalConfiguration().getCacheManagerName(), sessionCache.getName()));
        }
        SharedLocalYieldingClusterLockManager lockManager = this.lockManagerSource.getLockManager(sessionCache);
        BatchingManager batchingManager = new TransactionBatchingManager(sessionCache.getTransactionManager());
        SessionAttributeStorage<T> storage = this.storageFactory.createStorage(manager.getReplicationConfig().getReplicationGranularity(), this.marshallerFactory.createMarshaller(manager));

        return new DistributedCacheManager<T, SessionKeyImpl>(registry, manager, new AtomicMapCache<SessionKeyImpl, Object, Object>(sessionCache), this.jvmRouteCacheSource, lockManager, storage, batchingManager, new SessionKeyFactoryImpl(manager), this.invoker);
    }

    @Override
    public Collection<ServiceName> getDependencies(JBossWebMetaData metaData) {
        ReplicationConfig config = metaData.getReplicationConfig();
        if (config == null) {
            config = new ReplicationConfig();
        }
        return Arrays.asList(this.sessionCacheServiceNameProvider.getServiceName(config), this.jvmRouteCacheServiceNameProvider.getServiceName(config));
    }

    public void setSessionCacheSource(CacheSource source) {
        this.sessionCacheSource = source;
    }

    public void setJvmRouteCacheSource(CacheSource source) {
        this.jvmRouteCacheSource = source;
    }

    public void setLockManagerSource(LockManagerSource source) {
        this.lockManagerSource = source;
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
