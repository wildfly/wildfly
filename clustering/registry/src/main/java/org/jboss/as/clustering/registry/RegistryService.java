/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.registry;


import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.infinispan.invoker.BatchOperation;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 *
 */
@org.infinispan.notifications.Listener(sync = false)
public class RegistryService<K, V> implements Service<Registry<K, V>>, Registry<K, V> {

    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cacheRef = new InjectedValue<Cache>();
    private final RegistryEntryProvider<K, V> provider;
    private final Listener<K, V> listener;
    private volatile Cache<Address, Map.Entry<K, V>> cache;

    public RegistryService(RegistryEntryProvider<K, V> provider, Listener<K, V> listener) {
        this.provider = provider;
        this.listener = listener;
    }

    public ServiceBuilder<Registry<K, V>> build(ServiceTarget target, ServiceName serviceName, ServiceName cacheServiceName) {
        return target.addService(serviceName, this).addDependency(cacheServiceName, Cache.class, this.cacheRef);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public Registry<K, V> getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.clustering.registry.Registry#getEntries()
     */
    @Override
    public Map<K, V> getEntries() {
        Map<K, V> map = new HashMap<K, V>();
        for (Map.Entry<K, V> entry: this.cache.values()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void start(StartContext context) throws StartException {
        this.cache = this.cacheRef.getValue();
        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<Address, Map.Entry<K, V>> cache) {
                RegistryService.this.addCacheEntry(cache);
                return null;
            }
        };
        this.invoke(operation);
        this.cache.getCacheManager().addListener(this);
        this.cache.addListener(this);
    }

    void addCacheEntry(Cache<Address, Map.Entry<K, V>> cache) {
        K key = this.provider.getKey();
        if (key != null) {
            cache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP).put(cache.getCacheManager().getAddress(), new AbstractMap.SimpleImmutableEntry<K, V>(this.provider.getKey(), this.provider.getValue()));
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.cache.removeListener(this);
        this.cache.getCacheManager().removeListener(this);
        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<Address, Map.Entry<K, V>> cache) {
                cache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP).remove(cache.getCacheManager().getAddress());
                return null;
            }
        };
        this.invoke(operation);
    }

    @ViewChanged
    public void viewChanged(final ViewChangedEvent event) {
        Operation<Set<K>> operation = new Operation<Set<K>>() {
            @Override
            public Set<K> invoke(Cache<Address, Map.Entry<K, V>> cache) {
                Collection<Address> oldMembers = event.getOldMembers();
                Collection<Address> newMembers = event.getNewMembers();
                Set<K> removed = new HashSet<K>();
                // Remove entry of crashed member
                for (Address member: oldMembers) {
                    if (!newMembers.contains(member)) {
                        Map.Entry<K, V> old = cache.remove(member);
                        if (old != null) {
                            removed.add(old.getKey());
                        }
                    }
                }
                // Restore our entry in cache if we are joining (result of a split/merge)
                Address localAddress = event.getLocalAddress();
                if (!oldMembers.contains(localAddress) && newMembers.contains(localAddress)) {
                    RegistryService.this.addCacheEntry(cache);
                }
                return removed;
            }
        };

        Set<K> removed = this.invoke(operation);
        if (!removed.isEmpty() && (this.listener != null)) {
            listener.removedEntries(removed);
        }
    }

    @CacheEntryCreated
    public void created(CacheEntryCreatedEvent<Address, Map.Entry<K, V>> event) {
        if (event.isPre() || event.isOriginLocal()) return;
        if (this.listener != null) {
            Map.Entry<K, V> entry = event.getCache().get(event.getKey());
            if (entry != null) {
                this.listener.addedEntries(Collections.singletonMap(entry.getKey(), entry.getValue()));
            }
        }
    }

    @CacheEntryModified
    public void created(CacheEntryModifiedEvent<Address, Map.Entry<K, V>> event) {
        if (event.isPre() || event.isOriginLocal()) return;
        if (this.listener != null) {
            Map.Entry<K, V> entry = event.getCache().get(event.getKey());
            if (entry != null) {
                this.listener.updatedEntries(Collections.singletonMap(entry.getKey(), entry.getValue()));
            }
        }
    }

    @CacheEntryRemoved
    public void removed(CacheEntryRemovedEvent<Address, Map.Entry<K, V>> event) {
        // Need to run prior to removal, so the cache entry is available
        if (!event.isPre() || event.isOriginLocal()) return;
        if (this.listener != null) {
            Map.Entry<K, V> entry = event.getValue();
            if (entry != null) {
                this.listener.removedEntries(Collections.singleton(entry.getKey()));
            }
        }
    }

    <R> R invoke(Operation<R> operation) {
        return new BatchOperation<Address, Map.Entry<K, V>, R>(operation).invoke(this.cache);
    }

    abstract class Operation<R> implements CacheInvoker.Operation<Address, Map.Entry<K, V>, R> {
    }
}
