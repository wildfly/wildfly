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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.distribution.DataLocality;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.infinispan.invoker.BatchOperation;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager;
import org.jboss.as.clustering.registry.Registry;
import org.jboss.as.clustering.web.BatchingManager;
import org.jboss.as.clustering.web.DistributableSessionMetadata;
import org.jboss.as.clustering.web.IncomingDistributableSessionData;
import org.jboss.as.clustering.web.LocalDistributableSessionManager;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.clustering.web.SessionOwnershipSupport;
import org.jboss.as.clustering.web.impl.IncomingDistributableSessionDataImpl;

import static org.jboss.as.clustering.web.infinispan.InfinispanWebLogger.ROOT_LOGGER;
import static org.jboss.as.clustering.web.infinispan.InfinispanWebMessages.MESSAGES;

/**
 * Distributed cache manager implementation using Infinispan.
 *
 * @author Paul Ferraro
 */
@Listener
public class DistributedCacheManager<T extends OutgoingDistributableSessionData> implements org.jboss.as.clustering.web.DistributedCacheManager<T>, SessionOwnershipSupport {

    private static Map<SharedLocalYieldingClusterLockManager.LockResult, LockResult> results = lockResultMap();

    private static Map<SharedLocalYieldingClusterLockManager.LockResult, LockResult> lockResultMap() {
        Map<SharedLocalYieldingClusterLockManager.LockResult, LockResult> map = new EnumMap<SharedLocalYieldingClusterLockManager.LockResult, LockResult>(SharedLocalYieldingClusterLockManager.LockResult.class);
        map.put(SharedLocalYieldingClusterLockManager.LockResult.ACQUIRED_FROM_CLUSTER, LockResult.ACQUIRED_FROM_CLUSTER);
        map.put(SharedLocalYieldingClusterLockManager.LockResult.ALREADY_HELD, LockResult.ALREADY_HELD);
        map.put(SharedLocalYieldingClusterLockManager.LockResult.NEW_LOCK, LockResult.NEW_LOCK);
        return map;
    }

    final SessionAttributeStorage<T> attributeStorage;
    private final Random random = new Random(System.currentTimeMillis());
    private final LocalDistributableSessionManager manager;
    private final SharedLocalYieldingClusterLockManager lockManager;
    private final Cache<String, Map<Object, Object>> cache;
    private final ForceSynchronousCacheInvoker invoker;
    private final BatchingManager batchingManager;
    private final boolean passivationEnabled;
    private final Registry<String, Void> registry;
    private final long lockTimeout;

    public DistributedCacheManager(LocalDistributableSessionManager manager,
            Cache<String, Map<Object, Object>> cache, Registry<String, Void> registry,
            SharedLocalYieldingClusterLockManager lockManager, SessionAttributeStorage<T> attributeStorage,
            BatchingManager batchingManager, CacheInvoker invoker) {
        this.manager = manager;
        this.lockManager = lockManager;
        this.cache = cache;
        this.attributeStorage = attributeStorage;
        this.batchingManager = batchingManager;
        this.invoker = new ForceSynchronousCacheInvoker(invoker);
        this.lockTimeout = this.cache.getCacheConfiguration().locking().lockAcquisitionTimeout();

        Configuration configuration = this.cache.getCacheConfiguration();
        this.passivationEnabled = configuration.loaders().passivation() && !configuration.loaders().shared() && !configuration.loaders().cacheLoaders().isEmpty();
        this.registry = registry;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#start()
     */
    @Override
    public void start() {
        this.cache.start();
        this.cache.addListener(this);

        this.registry.refreshLocalEntry();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#stop()
     */
    @Override
    public void stop() {
        this.cache.removeListener(this);
        this.cache.stop();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#getBatchingManager()
     */
    @Override
    public BatchingManager getBatchingManager() {
        return this.batchingManager;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#sessionCreated(java.lang.String)
     */
    @Override
    public void sessionCreated(String sessionId) {
        this.trace("sessionCreated(%s)", sessionId);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#storeSessionData(org.jboss.web.tomcat.service.session.distributedcache.spi.OutgoingDistributableSessionData)
     */
    @Override
    public void storeSessionData(final T sessionData) {
        final String sessionId = sessionData.getRealId();

        this.trace("storeSessionData(%s)", sessionId);

        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<String, Map<Object, Object>> cache) {
                Map<Object, Object> map = cache.putIfAbsent(sessionId, null);

                SessionMapEntry.VERSION.put(map, Integer.valueOf(sessionData.getVersion()));
                SessionMapEntry.METADATA.put(map, sessionData.getMetadata());
                SessionMapEntry.TIMESTAMP.put(map, sessionData.getTimestamp());
                try {
                    DistributedCacheManager.this.attributeStorage.store(map, sessionData);
                } catch (IOException e) {
                    throw MESSAGES.failedToStoreSessionAttributes(e, sessionId);
                }
                return null;
            }
        };

        this.batch(operation);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#getSessionData(java.lang.String,
     *      boolean)
     */
    @Override
    public IncomingDistributableSessionData getSessionData(String sessionId, boolean initialLoad) {
        this.trace("getSessionData(%s, %s)", sessionId, initialLoad);

        return this.getData(sessionId, true);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#getSessionData(java.lang.String,
     *      java.lang.String, boolean)
     */
    @Override
    public IncomingDistributableSessionData getSessionData(final String sessionId, String dataOwner, boolean includeAttributes) {
        this.trace("getSessionData(%s, %s, %s)", sessionId, dataOwner, includeAttributes);

        return (dataOwner == null) ? this.getData(sessionId, includeAttributes) : null;
    }

    private IncomingDistributableSessionData getData(final String sessionId, final boolean includeAttributes) {
        Operation<IncomingDistributableSessionData> operation = new Operation<IncomingDistributableSessionData>() {
            @Override
            public IncomingDistributableSessionData invoke(Cache<String, Map<Object, Object>> cache) {
                Map<Object, Object> map = cache.get(sessionId);

                // If requested session is no longer in the cache; return null
                if (map == null) return null;

                Integer version = SessionMapEntry.VERSION.get(map);
                Long timestamp = SessionMapEntry.TIMESTAMP.get(map);
                DistributableSessionMetadata metadata = SessionMapEntry.METADATA.get(map);
                IncomingDistributableSessionDataImpl result = new IncomingDistributableSessionDataImpl(version, timestamp, metadata);

                if (includeAttributes) {
                    try {
                        result.setSessionAttributes(DistributedCacheManager.this.attributeStorage.load(map));
                    } catch (Exception e) {
                        throw MESSAGES.failedToLoadSessionAttributes(e, sessionId);
                    }
                }

                return result;
            }
        };

        try {
            return this.invoke(operation);
        } catch (Exception e) {
            ROOT_LOGGER.sessionLoadFailed(e, sessionId);

            // Clean up
            this.removeSessionLocal(sessionId);

            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#removeSession(java.lang.String)
     */
    @Override
    public void removeSession(final String sessionId) {
        this.trace("removeSession(%s)", sessionId);

        this.removeSession(sessionId, false);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#removeSessionLocal(java.lang.String)
     */
    @Override
    public void removeSessionLocal(final String sessionId) {
        this.trace("removeSessionLocal(%s)", sessionId);

        this.removeSession(sessionId, true);
    }

    private void removeSession(final String sessionId, final boolean local) {
        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<String, Map<Object, Object>> cache) {
                cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD, local ? Flag.CACHE_MODE_LOCAL : Flag.SKIP_REMOTE_LOOKUP).remove(sessionId);
                return null;
            }
        };
        this.batch(operation);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#removeSessionLocal(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public void removeSessionLocal(String sessionId, String dataOwner) {
        this.trace("removeSessionLocal(%s, dataOwner)", sessionId, dataOwner);

        if (dataOwner == null) {
            this.removeSession(sessionId, true);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#evictSession(java.lang.String)
     */
    @Override
    public void evictSession(final String sessionId) {
        this.trace("evictSession(%s)", sessionId);
        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<String, Map<Object, Object>> cache) {
                cache.evict(sessionId);
                return null;
            }
        };
        this.invoke(operation);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#evictSession(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public void evictSession(String sessionId, String dataOwner) {
        this.trace("evictSession(%s, %s)", sessionId, dataOwner);

        if (dataOwner == null) {
            this.evictSession(sessionId);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#getSessionIds()
     */
    @Override
    public Map<String, String> getSessionIds() {
        Map<String, String> result = new HashMap<String, String>();
        for (String sessionId: this.cache.keySet()) {
            result.put(sessionId, null);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#isPassivationEnabled()
     */
    @Override
    public boolean isPassivationEnabled() {
        return this.passivationEnabled;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#setForceSynchronous(boolean)
     */
    @Override
    public void setForceSynchronous(boolean forceSynchronous) {
        this.invoker.setForceSynchronous(forceSynchronous);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#getSessionOwnershipSupport()
     */
    @Override
    public SessionOwnershipSupport getSessionOwnershipSupport() {
        return (this.lockManager != null) ? this : null;
    }

    @Override
    public LockResult acquireSessionOwnership(String sessionId, boolean newLock) throws TimeoutException, InterruptedException {
        this.trace("acquireSessionOwnership(%s, %s)", sessionId, newLock);

        LockResult result = results.get(this.lockManager.lock(this.createLockKey(sessionId), this.lockTimeout, newLock));

        this.trace("acquireSessionOwnership(%s, %s) = %s", sessionId, newLock, result);

        return (result != null) ? result : LockResult.UNSUPPORTED;
    }

    @Override
    public void relinquishSessionOwnership(String sessionId, boolean remove) {
        this.trace("relinquishSessionOwnership(%s, %s)", sessionId, remove);

        this.lockManager.unlock(this.createLockKey(sessionId).toString(), remove);
    }

    private String createLockKey(String sessionId) {
        return this.cache.getName() + "/" + sessionId;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#isLocal(java.lang.String)
     */
    @Override
    public boolean isLocal(String sessionId) {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        if (dist != null) {
            DataLocality locality = dist.getLocality(sessionId);
            return locality.isLocal() || locality.isUncertain();
        }
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#locate(java.lang.String)
     */
    @Override
    public String locate(String sessionId) {
        if (!this.isLocal(sessionId)) {
            // Locate nodes on which the cache entry will reside
            List<Address> addresses = this.cache.getAdvancedCache().getDistributionManager().locate(sessionId);
            if (!addresses.contains(this.cache.getCacheManager().getAddress())) {
                // We need to force synchronous invocations to guarantee
                // session replicates before subsequent request.
                this.invoker.forceThreadSynchronous();
                // Otherwise choose random node from hash targets
                Map.Entry<String, Void> entry = this.registry.getRemoteEntry(addresses.get(this.random.nextInt(addresses.size())));
                if (entry != null) {
                    return entry.getKey();
                }
            }
        }
        return this.manager.getJvmRoute();
    }

    @CacheEntryRemoved
    public void removed(CacheEntryRemovedEvent<String, Map<Object, Object>> event) {
        if (event.isPre() || event.isOriginLocal()) return;

        try {
            this.manager.notifyRemoteInvalidation(event.getKey());
        } catch (Throwable e) {
            ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    @CacheEntryModified
    public void modified(CacheEntryModifiedEvent<String, Map<Object, Object>> event) {
        if (event.isPre() || event.isOriginLocal()) return;

        String sessionId = event.getKey();

        try {
            Map<Object, Object> map = event.getValue();
            if (!map.isEmpty()) {
                Integer version = SessionMapEntry.VERSION.get(map);
                Long timestamp = SessionMapEntry.TIMESTAMP.get(map);
                DistributableSessionMetadata metadata = SessionMapEntry.METADATA.get(map);

                if ((version != null) && (timestamp != null) && (metadata != null)) {
                    boolean updated = this.manager.sessionChangedInDistributedCache(sessionId, null, version.intValue(), timestamp.longValue(), metadata);

                    if (!updated) {
                        ROOT_LOGGER.versionIdMismatch(version, sessionId);
                    }
                }
            }
        } catch (Throwable e) {
            ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    @CacheEntryActivated
    public void activated(CacheEntryActivatedEvent<String, Map<Object, Object>> event) {
        if (event.isPre()) return;

        try {
            this.manager.sessionActivated();
        } catch (Throwable e) {
            ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    private void trace(String message, Object... args) {
        ROOT_LOGGER.tracef(message, args);
    }

    private <R> R batch(Operation<R> operation) {
        return this.invoke(new BatchOperation<String, Map<Object, Object>, R>(operation));
    }

    private <R> R invoke(CacheInvoker.Operation<String, Map<Object, Object>, R> operation) {
        return this.invoker.invoke(this.cache, operation);
    }

    // Simplified CacheInvoker.Operation using assigned key/value types
    abstract class Operation<R> implements CacheInvoker.Operation<String, Map<Object, Object>, R> {
    }

    static class ForceSynchronousCacheInvoker implements CacheInvoker {
        private static final ThreadLocal<Boolean> forceThreadSynchronous = new ThreadLocal<Boolean>() {
            @Override
            protected Boolean initialValue() {
                return Boolean.FALSE;
            }
        };

        private final CacheInvoker invoker;
        private volatile boolean forceSynchronous = false;

        ForceSynchronousCacheInvoker(CacheInvoker invoker) {
            this.invoker = invoker;
        }

        void setForceSynchronous(boolean forceSynchronous) {
            this.forceSynchronous = forceSynchronous;
        }

        void forceThreadSynchronous() {
            forceThreadSynchronous.set(Boolean.TRUE);
        }

        @Override
        public <K, V, R> R invoke(Cache<K, V> cache, Operation<K, V, R> operation) {
            return this.invoker.invoke(this.forceSynchronous || forceThreadSynchronous.get().booleanValue() ? cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS) : cache, operation);
        }
    }
}
