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

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.distribution.DataLocality;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.MarshalledValue;
import org.jboss.as.clustering.MarshalledValueFactory;
import org.jboss.as.clustering.infinispan.invoker.BatchOperation;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager.LockResult;
import org.jboss.as.clustering.lock.TimeoutException;
import org.jboss.as.clustering.registry.Registry;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.PassivationManager;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreConfig;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntry;
import org.jboss.as.ejb3.cache.spi.GroupCompatibilityChecker;
import org.jboss.as.ejb3.cache.spi.impl.AbstractBackingCacheEntryStore;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.NodeAffinity;
import org.jboss.logging.Logger;

/**
 * Infinispan-based backing cache entry store.
 * @author Paul Ferraro
 */
@Listener
public class InfinispanBackingCacheEntryStore<K extends Serializable, V extends Cacheable<K>, E extends BackingCacheEntry<K, V>, C> extends AbstractBackingCacheEntryStore<K, V, E>{
    private final Logger log = Logger.getLogger(getClass());

    private final SharedLocalYieldingClusterLockManager lockManager;
    private final LockKeyFactory<K, C> lockKeyFactory;
    private final MarshalledValueFactory<C> keyFactory;
    private final MarshalledValueFactory<C> valueFactory;
    private final C context;
    private final boolean controlCacheLifecycle;
    private final Cache<MarshalledValue<K, C>, MarshalledValue<E, C>> cache;
    private final CacheInvoker invoker;
    private final PassivationManager<K, E> passivationManager;
    private final boolean clustered;
    private final Random random = new Random(System.currentTimeMillis());
    private final Registry<String, ?> registry;

    public InfinispanBackingCacheEntryStore(Cache<MarshalledValue<K, C>, MarshalledValue<E, C>> cache, CacheInvoker invoker, PassivationManager<K, E> passivationManager, StatefulTimeoutInfo timeout, ClusteredBackingCacheEntryStoreConfig config, boolean controlCacheLifecycle, MarshalledValueFactory<C> keyFactory, MarshalledValueFactory<C> valueFactory, C context, SharedLocalYieldingClusterLockManager lockManager, LockKeyFactory<K, C> lockKeyFactory, Registry<String, ?> registry) {
        super(timeout, config);
        this.cache = cache;
        this.invoker = invoker;
        this.passivationManager = passivationManager;
        this.controlCacheLifecycle = controlCacheLifecycle;
        this.clustered = cache.getCacheConfiguration().clustering().cacheMode().isClustered();
        this.keyFactory = keyFactory;
        this.valueFactory = valueFactory;
        this.context = context;
        this.lockManager = this.clustered ? lockManager : null;
        this.lockKeyFactory = lockKeyFactory;
        this.registry = registry;
    }

    @Override
    public void start() {
        if (this.controlCacheLifecycle) {
            this.cache.start();
        }
    }

    @Override
    public void stop() {
        if (this.controlCacheLifecycle) {
            this.cache.stop();
        }
    }

    @Override
    public boolean hasAffinity(K key) {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        if (dist != null) {
            DataLocality locality = dist.getLocality(key);
            return locality.isLocal() || locality.isUncertain();
        }
        return true;
    }

    @Override
    public Affinity getStrictAffinity() {
        return new ClusterAffinity(this.cache.getCacheManager().getClusterName());
    }

    @Override
    public Affinity getWeakAffinity(K key) {
        if (!this.hasAffinity(key)) {
            // Locate nodes on which the cache entry will reside
            List<Address> addresses = this.cache.getAdvancedCache().getDistributionManager().locate(key);
            if (!addresses.contains(this.cache.getCacheManager().getAddress())) {
                // Otherwise choose random node from hash targets
                Map.Entry<String, ?> entry = this.registry.getRemoteEntry(addresses.get(random.nextInt(addresses.size())));
                if (entry != null) {
                    return new NodeAffinity(entry.getKey());
                }
            }
        }
        return new NodeAffinity(this.registry.getLocalEntry().getKey());
    }

    @Override
    public void insert(E entry) {
        K id = entry.getId();
        this.trace("insert(%s)", id);
        final MarshalledValue<K, C> key = this.marshalKey(id);

        this.acquireSessionOwnership(key, true);
        try {
            final MarshalledValue<E, C> value = this.marshalEntry(entry);
            Operation<Void> operation = new Operation<Void>() {
                @Override
                public Void invoke(Cache<MarshalledValue<K, C>, MarshalledValue<E, C>> cache) {
                    cache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP).put(key, value);
                    return null;
                }
            };
            this.invoke(operation);
        } finally {
            this.releaseSessionOwnership(key, false);
        }
    }

    @Override
    public E get(K id, boolean lock) {
        this.trace("get(%s. %s)", id, lock);
        final MarshalledValue<K, C> key = this.marshalKey(id);

        if (lock) {
            this.acquireSessionOwnership(key, false);
        }

        Operation<MarshalledValue<E, C>> operation = new Operation<MarshalledValue<E, C>>() {
            @Override
            public MarshalledValue<E, C> invoke(Cache<MarshalledValue<K, C>, MarshalledValue<E, C>> cache) {
                return cache.get(key);
            }
        };
        return this.unmarshalEntry(id, this.invoke(operation));
    }

    @Override
    public void update(E entry, boolean modified) {
        K id = entry.getId();
        this.trace("update(%s, %s)", id, modified);
        final MarshalledValue<K, C> key = this.marshalKey(id);
        try {
            if (modified) {
                final MarshalledValue<E, C> value = this.marshalEntry(entry);
                Operation<Void> operation = new Operation<Void>() {
                    @Override
                    public Void invoke(Cache<MarshalledValue<K, C>, MarshalledValue<E, C>> cache) {
                        cache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP).put(key, value);
                        return null;
                    }
                };
                this.invoke(operation);
            }
        } finally {
            this.releaseSessionOwnership(key, false);
        }
    }

    @Override
    public E remove(K id) {
        this.trace("remove(%s)", id);
        final MarshalledValue<K, C> key = this.marshalKey(id);
        Operation<MarshalledValue<E, C>> operation = new Operation<MarshalledValue<E, C>>() {
            @Override
            public MarshalledValue<E, C> invoke(Cache<MarshalledValue<K, C>, MarshalledValue<E, C>> cache) {
                return cache.remove(key);
            }
        };
        try {
            return this.unmarshalEntry(id, this.invoke(operation));
        } finally {
            this.releaseSessionOwnership(key, true);
        }
    }

    private <R> R invoke(Operation<R> operation) {
        return this.invoker.invoke(this.cache, new BatchOperation<MarshalledValue<K, C>, MarshalledValue<E, C>, R>(operation));
    }

    private MarshalledValue<K, C> marshalKey(K key) {
        try {
            return this.keyFactory.createMarshalledValue(key);
        } catch (IOException e) {
            throw InfinispanEjbMessages.MESSAGES.serializationFailure(e, key);
        }
    }

    private K unmarshalKey(MarshalledValue<K, C> key) {
        try {
            return key.get(this.context);
        } catch (Exception e) {
            throw InfinispanEjbMessages.MESSAGES.deserializationFailure(e, key);
        }
    }

    private MarshalledValue<E, C> marshalEntry(E value) {
        try {
            return this.valueFactory.createMarshalledValue(value);
        } catch (IOException e) {
            throw InfinispanEjbMessages.MESSAGES.serializationFailure(e, value.getId());
        }
    }

    private E unmarshalEntry(K id, MarshalledValue<E, C> value) {
        if (value == null) return null;
        try {
            return value.get(this.context);
        } catch (Exception e) {
            throw InfinispanEjbMessages.MESSAGES.deserializationFailure(e, id);
        }
    }

    private LockResult acquireSessionOwnership(MarshalledValue<K, C> key, boolean newLock) {
        if (this.lockManager == null) return null;

        Serializable lockKey = this.lockKeyFactory.createLockKey(key);

        this.trace("Acquiring %slock on %s", newLock ? "new " : "", lockKey);

        long timeout = this.cache.getCacheConfiguration().locking().lockAcquisitionTimeout();
        try {
            LockResult result = this.lockManager.lock(lockKey, timeout, newLock);
            this.trace("Lock acquired (%s) on %s", result, lockKey);
            return result;
        } catch (TimeoutException e) {
            throw InfinispanEjbMessages.MESSAGES.lockAcquisitionTimeout(lockKey, timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw InfinispanEjbMessages.MESSAGES.lockAcquisitionInterruption(e, lockKey);
        }
    }

    private void releaseSessionOwnership(MarshalledValue<K, C> key, boolean remove) {
        if (this.lockManager != null) {
            Serializable lockKey = this.lockKeyFactory.createLockKey(key);
            this.trace("Releasing %slock on %s", remove ? "and removing " : "", lockKey);
            this.lockManager.unlock(lockKey, remove);
            this.trace("Released %slock on %s", remove ? "and removed " : "", lockKey);
        }
    }

    @Override
    public void passivate(E entry) {
        final MarshalledValue<K, C> key = this.marshalKey(entry.getId());
        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<MarshalledValue<K, C>, MarshalledValue<E, C>> cache) {
                cache.evict(key);
                return null;
            }
        };
        this.invoker.invoke(this.cache, operation);
    }

    @Override
    public boolean isClustered() {
        return this.clustered;
    }

    @Override
    public boolean isCompatibleWith(GroupCompatibilityChecker other) {
        if (other instanceof InfinispanBackingCacheEntryStore) {
            InfinispanBackingCacheEntryStore<?, ?, ?, ?> store = (InfinispanBackingCacheEntryStore<?, ?, ?, ?>) other;
            return this.cache.getCacheManager() == store.cache.getCacheManager();
        }
        return false;
    }

    @CacheEntryActivated
    public void activated(CacheEntryActivatedEvent<MarshalledValue<K, C>, MarshalledValue<E, C>> event) {
        if ((this.passivationManager != null) && !event.isPre()){
            K key = this.unmarshalKey(event.getKey());
            this.trace("activated(%s)", key);
            this.passivationManager.postActivate(this.unmarshalEntry(key, event.getValue()));
        }
    }

    @CacheEntryPassivated
    public void passivated(CacheEntryPassivatedEvent<MarshalledValue<K, C>, MarshalledValue<E, C>> event) {
        if ((this.passivationManager != null) && event.isPre()) {
            K key = this.unmarshalKey(event.getKey());
            this.trace("passivated(%s)", key);
            this.passivationManager.prePassivate(this.unmarshalEntry(key, event.getValue()));
        }
    }

    abstract class Operation<R> implements CacheInvoker.Operation<MarshalledValue<K, C>, MarshalledValue<E, C>, R> {
    }

    private void trace(String message, Object... args) {
        if (this.log.isTraceEnabled()) {
            this.log.tracef(message, args);
        }
    }
}
