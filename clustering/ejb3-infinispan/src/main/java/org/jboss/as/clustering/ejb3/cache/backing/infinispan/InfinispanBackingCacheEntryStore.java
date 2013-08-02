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
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.context.Flag;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.persistence.CacheLoaderException;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.marshalling.MarshalledValue;
import org.jboss.as.clustering.marshalling.MarshalledValueFactory;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.IdentifierFactory;
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
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.registry.Registry;

/**
 * Infinispan-based backing cache entry store.
 * @author Paul Ferraro
 */
@Listener
public class InfinispanBackingCacheEntryStore<K extends Serializable, V extends Cacheable<K>, E extends BackingCacheEntry<K, V>, C> extends AbstractBackingCacheEntryStore<K, V, E> implements KeyGenerator<K> {
    private final Logger log = Logger.getLogger(getClass());

    private final MarshalledValueFactory<C> valueFactory;
    private final C context;
    private final boolean controlCacheLifecycle;
    private final Cache<K, MarshalledValue<E, C>> cache;
    private final CacheInvoker invoker;
    private final PassivationManager<K, E> passivationManager;
    private final boolean clustered;
    private final NodeFactory<Address> nodeFactory;
    private final Registry<String, ?> registry;
    private final IdentifierFactory<K> identifierFactory;
    private final KeyAffinityService<K> affinity;

    public InfinispanBackingCacheEntryStore(Cache<K, MarshalledValue<E, C>> cache, CacheInvoker invoker, IdentifierFactory<K> identifierFactory, KeyAffinityServiceFactory affinityFactory, PassivationManager<K, E> passivationManager, StatefulTimeoutInfo timeout, ClusteredBackingCacheEntryStoreConfig config, boolean controlCacheLifecycle, MarshalledValueFactory<C> valueFactory, C context, NodeFactory<Address> nodeFactory, Registry<String, ?> registry) {
        super(timeout, config);
        this.cache = cache;
        this.invoker = invoker;
        this.identifierFactory = identifierFactory;
        this.passivationManager = passivationManager;
        this.controlCacheLifecycle = controlCacheLifecycle;
        this.clustered = cache.getCacheConfiguration().clustering().cacheMode().isClustered();
        this.valueFactory = valueFactory;
        this.context = context;
        this.nodeFactory = nodeFactory;
        this.registry = registry;
        this.affinity = affinityFactory.createService(cache, this);
    }

    @Override
    public void start() {
        if (this.controlCacheLifecycle) {
            this.cache.start();
        }
        this.affinity.start();
    }

    @Override
    public void stop() {
        this.affinity.stop();
        if (this.controlCacheLifecycle) {
            this.cache.stop();
        }
    }

    @Override
    public K createIdentifier() {
        return this.affinity.getKeyForAddress(this.cache.getCacheManager().getAddress());
    }

    @Override
    public K getKey() {
        return this.identifierFactory.createIdentifier();
    }

    @Override
    public boolean hasAffinity(K key) {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        if (dist != null) {
            return dist.getPrimaryLocation(key).equals(this.cache.getCacheManager().getAddress());
        }
        return true;
    }

    @Override
    public Affinity getStrictAffinity() {
        return new ClusterAffinity(this.cache.getCacheManager().getClusterName());
    }

    @Override
    public Affinity getWeakAffinity(K key) {
        if (this.registry == null) return Affinity.NONE;
        Map.Entry<String, ?> entry = null;
        Address location = this.locatePrimaryOwner(key);
        if ((location != null) && (this.nodeFactory != null)) {
            Node node = this.nodeFactory.createNode(location);
            entry = this.registry.getEntry(node);
        }
        if (entry == null) {
            entry = this.registry.getLocalEntry();
        }
        return (entry != null) ? new NodeAffinity(entry.getKey()) : Affinity.NONE;
    }

    private Address locatePrimaryOwner(K key) {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        return (dist != null) ? dist.getPrimaryLocation(key) : null;
    }

    @Override
    public Set<K> insert(E entry) {
        final K id = entry.getId();
        this.trace("insert(%s)", id);

        final MarshalledValue<E, C> value = this.marshalEntry(entry);
        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<K, MarshalledValue<E, C>> cache) {
                cache.put(id, value);
                return null;
            }
        };
        this.invoker.invoke(this.cache, operation, Flag.SKIP_REMOTE_LOOKUP);
        return Collections.emptySet();
    }

    @Override
    public E get(final K id, boolean lock) {
        this.trace("get(%s. %s)", id, lock);

        Operation<MarshalledValue<E, C>> operation = new Operation<MarshalledValue<E, C>>() {
            @Override
            public MarshalledValue<E, C> invoke(Cache<K, MarshalledValue<E, C>> cache) {
                return cache.get(id);
            }
        };
        return this.unmarshalEntry(id, this.invoker.invoke(this.cache, operation));
    }

    @Override
    public void update(E entry, boolean modified) {
        final K id = entry.getId();
        this.trace("update(%s, %s)", id, modified);
        if (modified) {
            final MarshalledValue<E, C> value = this.marshalEntry(entry);
            Operation<Void> operation = new Operation<Void>() {
                @Override
                public Void invoke(Cache<K, MarshalledValue<E, C>> cache) {
                    cache.put(id, value);
                    return null;
                }
            };
            this.invoker.invoke(this.cache, operation, Flag.SKIP_REMOTE_LOOKUP);
        }
    }

    @Override
    public E remove(final K id) {
        this.trace("remove(%s)", id);
        Operation<MarshalledValue<E, C>> operation = new Operation<MarshalledValue<E, C>>() {
            @Override
            public MarshalledValue<E, C> invoke(Cache<K, MarshalledValue<E, C>> cache) {
                return cache.remove(id);
            }
        };
        return this.unmarshalEntry(id, this.invoker.invoke(this.cache, operation));
    }

    private K unmarshalKey(MarshalledValue<K, C> key) {
        try {
            return key.get(this.context);
        } catch (Exception e) {
            throw InfinispanEjbMessages.MESSAGES.deserializationFailure(e, key);
        }
    }

    private MarshalledValue<E, C> marshalEntry(E value) {
        return this.valueFactory.createMarshalledValue(value);
    }

    private E unmarshalEntry(K id, MarshalledValue<E, C> value) {
        if (value == null) return null;
        try {
            return value.get(this.context);
        } catch (Exception e) {
            throw InfinispanEjbMessages.MESSAGES.deserializationFailure(e, id);
        }
    }

    @Override
    public void passivate(final E entry) {
        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<K, MarshalledValue<E, C>> cache) {
                cache.evict(entry.getId());
                return null;
            }
        };
        this.invoker.invoke(this.cache, operation, Flag.FAIL_SILENTLY);
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

    abstract class Operation<R> implements CacheInvoker.Operation<K, MarshalledValue<E, C>, R> {
    }

    private void trace(String message, Object... args) {
        if (this.log.isTraceEnabled()) {
            this.log.tracef(message, args);
        }
    }

    @Override
    public int getStoreSize() {
        return this.cache.size();
    }

    @Override
    public int getPassivatedCount() {
        try {
            return this.cache.getAdvancedCache().getComponentRegistry().getComponent(PersistenceManager.class).size();
        } catch (CacheLoaderException e) {
            throw InfinispanEjbMessages.MESSAGES.cacheLoaderFailure(e);
        }
    }

}
