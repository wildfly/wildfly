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
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.distribution.DataLocality;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheStoreConfig;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.infinispan.invoker.BatchOperation;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager;
import org.jboss.as.clustering.web.BatchingManager;
import org.jboss.as.clustering.web.DistributableSessionMetadata;
import org.jboss.as.clustering.web.IncomingDistributableSessionData;
import org.jboss.as.clustering.web.LocalDistributableSessionManager;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.clustering.web.SessionOwnershipSupport;
import org.jboss.as.clustering.web.impl.IncomingDistributableSessionDataImpl;
import org.jboss.msc.service.ServiceRegistry;

import static org.jboss.as.clustering.web.infinispan.InfinispanWebLogger.ROOT_LOGGER;
import static org.jboss.as.clustering.web.infinispan.InfinispanWebMessages.MESSAGES;

/**
 * Distributed cache manager implementation using Infinispan.
 *
 * @author Paul Ferraro
 */
@Listener
public class DistributedCacheManager<T extends OutgoingDistributableSessionData, K extends SessionKey> implements org.jboss.as.clustering.web.DistributedCacheManager<T>, SessionOwnershipSupport {
    static String mask(String sessionId) {
        if (sessionId == null) return null;
        int length = sessionId.length();
        if (length <= 8) {
            return sessionId;
        }
        return sessionId.substring(0, 2) + "****" + sessionId.substring(length - 6, length);
    }

    private static final Random random = new Random();

    private static Map<SharedLocalYieldingClusterLockManager.LockResult, LockResult> results = lockResultMap();

    private static Map<SharedLocalYieldingClusterLockManager.LockResult, LockResult> lockResultMap() {
        Map<SharedLocalYieldingClusterLockManager.LockResult, LockResult> map = new EnumMap<SharedLocalYieldingClusterLockManager.LockResult, LockResult>(SharedLocalYieldingClusterLockManager.LockResult.class);
        map.put(SharedLocalYieldingClusterLockManager.LockResult.ACQUIRED_FROM_CLUSTER, LockResult.ACQUIRED_FROM_CLUSTER);
        map.put(SharedLocalYieldingClusterLockManager.LockResult.ALREADY_HELD, LockResult.ALREADY_HELD);
        map.put(SharedLocalYieldingClusterLockManager.LockResult.NEW_LOCK, LockResult.NEW_LOCK);
        return map;
    }

    final SessionAttributeStorage<T> attributeStorage;
    private final LocalDistributableSessionManager manager;
    private final SharedLocalYieldingClusterLockManager lockManager;
    private final Cache<K, Map<Object, Object>> sessionCache;
    private final ForceSynchronousCacheInvoker invoker;
    private final BatchingManager batchingManager;
    private final boolean passivationEnabled;
    private final boolean requiresPurge;
    private final JvmRouteHandler jvmRouteHandler;
    private final SessionKeyFactory<K> keyFactory;

    public DistributedCacheManager(ServiceRegistry registry, LocalDistributableSessionManager manager,
            Cache<K, Map<Object, Object>> sessionCache, CacheSource jvmRouteCacheSource,
            SharedLocalYieldingClusterLockManager lockManager, SessionAttributeStorage<T> attributeStorage,
            BatchingManager batchingManager, SessionKeyFactory<K> keyFactory, CacheInvoker invoker) {
        this.manager = manager;
        this.lockManager = lockManager;
        this.sessionCache = sessionCache;
        this.attributeStorage = attributeStorage;
        this.batchingManager = batchingManager;
        this.keyFactory = keyFactory;
        this.invoker = new ForceSynchronousCacheInvoker(invoker);

        Configuration configuration = this.sessionCache.getConfiguration();
        Configuration.CacheMode mode = configuration.getCacheMode();
        this.passivationEnabled = configuration.isCacheLoaderPassivation() && !configuration.isCacheLoaderShared();
        List<CacheLoaderConfig> loaders = configuration.getCacheLoaders();
        CacheLoaderConfig loader = !loaders.isEmpty() ? loaders.get(0) : null;
        this.requiresPurge = (loader != null) && (loader instanceof CacheStoreConfig) ? ((CacheStoreConfig) loader).isPurgeOnStartup() && mode.isReplicated() : false;
        this.jvmRouteHandler = mode.isDistributed() ? new JvmRouteHandler(registry, jvmRouteCacheSource, this.manager) : null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#start()
     */
    @Override
    public void start() {
        this.sessionCache.addListener(this);

        if (this.jvmRouteHandler != null) {
            EmbeddedCacheManager container = this.sessionCache.getCacheManager();

            container.addListener(this.jvmRouteHandler);

            String jvmRoute = this.manager.getJvmRoute();

            if (jvmRoute != null) {
                this.jvmRouteHandler.getCache().putIfAbsent(container.getAddress(), jvmRoute);
            }
        }
    }

    private void purge() {
        if (this.requiresPurge) {
            Operation<Void> operation = new Operation<Void>() {
                @Override
                public Void invoke(Cache<K, Map<Object, Object>> cache) {
                    for (K key: cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).keySet()) {
                        if (DistributedCacheManager.this.keyFactory.ours(key)) {
                            cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).remove(key);
                        }
                    }
                    return null;
                }
            };

            this.batch(operation);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#stop()
     */
    @Override
    public void stop() {
        if (this.jvmRouteHandler != null) {
            this.sessionCache.getCacheManager().removeListener(this.jvmRouteHandler);
        }
        this.sessionCache.removeListener(this);
        // this.purge();
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
        ROOT_LOGGER.tracef("sessionCreated(%s)", sessionId);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#storeSessionData(org.jboss.web.tomcat.service.session.distributedcache.spi.OutgoingDistributableSessionData)
     */
    @Override
    public void storeSessionData(final T sessionData) {
        final K key = this.keyFactory.createKey(sessionData.getRealId());

        ROOT_LOGGER.tracef("storeSessionData(%s)", key.getSessionId());

        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<K, Map<Object, Object>> cache) {
                Map<Object, Object> map = cache.putIfAbsent(key, null);

                SessionMapEntry.VERSION.put(map, Integer.valueOf(sessionData.getVersion()));
                SessionMapEntry.METADATA.put(map, sessionData.getMetadata());
                SessionMapEntry.TIMESTAMP.put(map, sessionData.getTimestamp());
                try {
                    DistributedCacheManager.this.attributeStorage.store(map, sessionData);
                } catch (IOException e) {
                    throw MESSAGES.failedToStoreSessionAttributes(e, mask(key.getSessionId()));
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
        ROOT_LOGGER.tracef("getSessionData(%s, %s)", sessionId, initialLoad);

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
        ROOT_LOGGER.tracef("getSessionData(%s, %s, %s)", sessionId, dataOwner, includeAttributes);

        return (dataOwner == null) ? this.getData(sessionId, includeAttributes) : null;
    }

    private IncomingDistributableSessionData getData(String sessionId, final boolean includeAttributes) {
        final K key = this.keyFactory.createKey(sessionId);
        Operation<IncomingDistributableSessionData> operation = new Operation<IncomingDistributableSessionData>() {
            @Override
            public IncomingDistributableSessionData invoke(Cache<K, Map<Object, Object>> cache) {
                Map<Object, Object> map = cache.get(key);

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
                        throw MESSAGES.failedToStoreSessionAttributes(e, mask(key.getSessionId()));
                    }
                }

                return result;
            }
        };

        try {
            return this.invoker.invoke(this.sessionCache, operation);
        } catch (Exception e) {
            ROOT_LOGGER.errorAccessingSession(e, mask(sessionId), e.getLocalizedMessage());
            ROOT_LOGGER.errorAccessingSession(mask(sessionId), e.getLocalizedMessage());

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
        ROOT_LOGGER.tracef("removeSession(%s)", sessionId);

        this.removeSession(sessionId, false);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#removeSessionLocal(java.lang.String)
     */
    @Override
    public void removeSessionLocal(final String sessionId) {
        ROOT_LOGGER.tracef("removeSessionLocal(%s)", sessionId);

        this.removeSession(sessionId, true);
    }

    private void removeSession(final String sessionId, final boolean local) {
        final K key = this.keyFactory.createKey(sessionId);
        Operation<Map<Object, Object>> operation = new Operation<Map<Object, Object>>() {
            @Override
            public Map<Object, Object> invoke(Cache<K, Map<Object, Object>> cache) {
                return cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD, local ? Flag.CACHE_MODE_LOCAL : Flag.SKIP_REMOTE_LOOKUP).remove(key);
            }
        };

        this.invoker.invoke(this.sessionCache, operation);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#removeSessionLocal(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public void removeSessionLocal(String sessionId, String dataOwner) {
        ROOT_LOGGER.tracef("removeSessionLocal(%s, dataOwner)", sessionId, dataOwner);

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
    public void evictSession(String sessionId) {
        ROOT_LOGGER.tracef("evictSession(%s)", sessionId);
        final K key = this.keyFactory.createKey(sessionId);
        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<K, Map<Object, Object>> cache) {
                cache.evict(key);
                return null;
            }
        };

        this.invoker.invoke(this.sessionCache, operation);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#evictSession(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public void evictSession(String sessionId, String dataOwner) {
        ROOT_LOGGER.tracef("evictSession(%s, %s)", sessionId, dataOwner);

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
        for (K key: this.sessionCache.keySet()) {
            if (this.keyFactory.ours(key)) {
                result.put(key.getSessionId(), null);
            }
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
        ROOT_LOGGER.tracef("acquireSessionOwnership(%s, %s)", sessionId, newLock);

        EmbeddedCacheManager container = (EmbeddedCacheManager) this.sessionCache.getCacheManager();

        LockResult result = results.get(this.lockManager.lock(this.keyFactory.createKey(sessionId).toString(), container.getGlobalConfiguration().getDistributedSyncTimeout(), newLock));

        ROOT_LOGGER.tracef("acquireSessionOwnership(%s, %s) = %s", sessionId, newLock, result);

        return (result != null) ? result : LockResult.UNSUPPORTED;
    }

    @Override
    public void relinquishSessionOwnership(String sessionId, boolean remove) {
        ROOT_LOGGER.tracef("relinquishSessionOwnership(%s, %s)", sessionId, remove);

        this.lockManager.unlock(this.keyFactory.createKey(sessionId).toString(), remove);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#isLocal(java.lang.String)
     */
    @Override
    public boolean isLocal(String sessionId) {
        DistributionManager manager = this.sessionCache.getAdvancedCache().getDistributionManager();

        if (manager == null) return true;

        DataLocality locality = manager.getLocality(sessionId);

        return locality.isLocal() || locality.isUncertain();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.web.tomcat.service.session.distributedcache.spi.DistributedCacheManager#locate(java.lang.String)
     */
    @Override
    public String locate(String sessionId) {
        if (this.jvmRouteHandler != null) {
            AdvancedCache<?, ?> cache = this.sessionCache.getAdvancedCache();
            DistributionManager manager = cache.getDistributionManager();

            // If rehash is in progress, just use our jvm route - don't hold up the request
            if ((manager != null) && !manager.isRehashInProgress()) {
                List<Address> addresses = manager.locate(sessionId);

                EmbeddedCacheManager container = (EmbeddedCacheManager) this.sessionCache.getCacheManager();

                // Prefer this node, if session happens to hash here
                if (!addresses.contains(container.getAddress())) {
                    // Otherwise choose random node from hash targets
                    Address address = addresses.get(random.nextInt(addresses.size()));

                    String jvmRoute = this.jvmRouteHandler.getCache().get(address);

                    if (jvmRoute != null) {
                        ROOT_LOGGER.tracef("%s hashes to %s - next request will route to %s (%s)", sessionId, addresses, address, jvmRoute);

                        // We need to force synchronous invocations to guarantee
                        // session replicates before subsequent request.
                        this.invoker.forceThreadSynchronous();
                        return jvmRoute;
                    }
                }
            }
        }

        return this.manager.getJvmRoute();
    }

    @CacheEntryRemoved
    public void removed(CacheEntryRemovedEvent<K, Map<Object, Object>> event) {
        if (event.isPre() || event.isOriginLocal()) return;

        K key = event.getKey();

        if (this.keyFactory.ours(key)) {
            try {
                this.manager.notifyRemoteInvalidation(key.getSessionId());
            } catch (Throwable e) {
                ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            }
        }
    }

    @CacheEntryModified
    public void modified(CacheEntryModifiedEvent<K, Map<Object, Object>> event) {
        if (event.isPre() || event.isOriginLocal()) return;

        K key = event.getKey();

        if (this.keyFactory.ours(key)) {
            try {
                Map<Object, Object> map = event.getValue();

                if (!map.isEmpty()) {
                    String sessionId = key.getSessionId();

                    Integer version = SessionMapEntry.VERSION.get(map);
                    Long timestamp = SessionMapEntry.TIMESTAMP.get(map);
                    DistributableSessionMetadata metadata = SessionMapEntry.METADATA.get(map);

                    if ((version != null) && (timestamp != null) && (metadata != null)) {
                        boolean updated = this.manager.sessionChangedInDistributedCache(sessionId, null, version.intValue(), timestamp.longValue(), metadata);

                        if (!updated) {
                            ROOT_LOGGER.versionIdMismatch(version, mask(sessionId));
                        }
                    }
                }
            } catch (Throwable e) {
                ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            }
        }
    }

    @CacheEntryActivated
    public void activated(CacheEntryActivatedEvent<K, Map<Object, Object>> event) {
        if (event.isPre()) return;

        K key = event.getKey();

        if (this.keyFactory.ours(key)) {
            try {
                this.manager.sessionActivated();
            } catch (Throwable e) {
                ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            }
        }
    }

    private <R> R batch(Operation<R> operation) {
        return this.invoker.invoke(this.sessionCache, new BatchOperation<K, Map<Object, Object>, R>(operation));
    }

    @Listener(sync = false)
    public static class JvmRouteHandler {
        // Simplified CacheInvoker.Operation using assigned key/value types
        static interface Operation<R> extends CacheInvoker.Operation<Address, String, R> {
        }

        private final ServiceRegistry registry;
        private final LocalDistributableSessionManager manager;
        private final CacheSource source;

        JvmRouteHandler(ServiceRegistry registry, CacheSource source, LocalDistributableSessionManager manager) {
            this.registry = registry;
            this.source = source;
            this.manager = manager;
        }

        Cache<Address, String> getCache() {
            return this.source.getCache(this.registry, this.manager);
        }

        <R> R batch(Operation<R> operation) {
            Cache<Address, String> cache = this.getCache();
            boolean started = cache.startBatch();
            boolean success = false;
            try {
                R result = operation.invoke(cache);
                success = true;
                return result;
            } finally {
                if (started) {
                    cache.endBatch(success);
                }
            }
        }

        @ViewChanged
        public void viewChanged(final ViewChangedEvent event) {
            final Collection<Address> oldMembers = event.getOldMembers();
            final Collection<Address> newMembers = event.getNewMembers();
            final String jvmRoute = this.manager.getJvmRoute();

            Operation<Void> operation = new Operation<Void>() {
                @Override
                public Void invoke(Cache<Address, String> cache) {
                    // Remove jvm route of crashed member
                    for (Address member: oldMembers) {
                        if (!newMembers.contains(member)) {
                            if (cache.remove(member) != null) {
                                ROOT_LOGGER.removedJvmRouteEntry(member);
                            }
                        }
                    }

                    // Restore our jvm route in cache if we are joining (result of a split/merge)
                    if (jvmRoute != null) {
                        Address localAddress = event.getLocalAddress();
                        if (!oldMembers.contains(localAddress) && newMembers.contains(localAddress)) {
                            String oldJvmRoute = cache.put(localAddress, jvmRoute);
                            if (oldJvmRoute == null) {
                                ROOT_LOGGER.addingJvmRouteEntry();
                            } else if (!oldJvmRoute.equals(jvmRoute)) {
                                ROOT_LOGGER.updatingJvmRouteEntry(oldJvmRoute, jvmRoute);
                            }
                        }
                    }
                    return null;
                }
            };

            try {
                this.batch(operation);
            } catch (Throwable e) {
                ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            }
        }
    }

    // Simplified CacheInvoker.Operation using assigned key/value types
    abstract class Operation<R> implements CacheInvoker.Operation<K, Map<Object, Object>, R> {
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
