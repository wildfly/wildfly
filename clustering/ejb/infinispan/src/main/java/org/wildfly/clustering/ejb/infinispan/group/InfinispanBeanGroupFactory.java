/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan.group;

import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.ee.infinispan.CacheEntryMutator;
import org.wildfly.clustering.ee.infinispan.Mutator;
import org.wildfly.clustering.ejb.infinispan.BeanEntry;
import org.wildfly.clustering.ejb.infinispan.BeanGroup;
import org.wildfly.clustering.ejb.infinispan.BeanGroupEntry;
import org.wildfly.clustering.ejb.infinispan.BeanGroupFactory;
import org.wildfly.clustering.ejb.infinispan.BeanGroupKey;
import org.wildfly.clustering.ejb.infinispan.BeanKey;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanKey;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.marshalling.jboss.MarshalledValueFactory;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;

/**
 * Encapsulates the cache mapping strategy of a bean group.
 *
 * @author Paul Ferraro
 *
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class InfinispanBeanGroupFactory<I, T> implements BeanGroupFactory<I, T> {

    private static final Flag[] EVICTION_FLAGS = new Flag[] { Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD, Flag.ZERO_LOCK_ACQUISITION_TIMEOUT, Flag.FAIL_SILENTLY };
    private final Cache<BeanGroupKey<I>, BeanGroupEntry<I, T>> cache;
    private final Cache<BeanGroupKey<I>, BeanGroupEntry<I, T>> findCache;
    private final Cache<BeanKey<I>, BeanEntry<I>> beanCache;
    private final MarshalledValueFactory<MarshallingContext> factory;
    private final MarshallingContext context;

    public InfinispanBeanGroupFactory(Cache<BeanGroupKey<I>, BeanGroupEntry<I, T>> cache, Cache<BeanKey<I>, BeanEntry<I>> beanCache, MarshalledValueFactory<MarshallingContext> factory, MarshallingContext context, CacheProperties properties) {
        this.cache = cache;
        this.findCache = properties.isLockOnRead() ? cache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK) : cache;
        this.beanCache = properties.isLockOnRead() ? beanCache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK) : beanCache;
        this.factory = factory;
        this.context = context;
    }

    @Override
    public BeanGroupKey<I> createKey(I id) {
        return new InfinispanBeanGroupKey<>(id);
    }

    @Override
    public BeanGroupEntry<I, T> createValue(I id, Void context) {
        return this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).computeIfAbsent(this.createKey(id), key -> new InfinispanBeanGroupEntry<>(this.factory.createMarshalledValue(new ConcurrentHashMap<>())));
    }

    @Override
    public BeanGroupEntry<I, T> findValue(I id) {
        return this.findCache.get(this.createKey(id));
    }

    @Override
    public BeanGroupEntry<I, T> tryValue(I id) {
        return this.findCache.getAdvancedCache().withFlags(Flag.ZERO_LOCK_ACQUISITION_TIMEOUT, Flag.FAIL_SILENTLY).get(this.createKey(id));
    }

    @Override
    public void evict(I id) {
        BeanGroupKey<I> key = this.createKey(id);
        BeanGroupEntry<I, T> entry = this.findCache.getAdvancedCache().withFlags(EVICTION_FLAGS).get(key);
        if (entry != null) {
            try {
                for (I beanId : entry.getBeans().get(this.context).keySet()) {
                    BeanKey<I> beanKey = new InfinispanBeanKey<>(beanId);
                    if (this.beanCache.getAdvancedCache().withFlags(EVICTION_FLAGS).get(beanKey) != null) {
                        this.beanCache.evict(beanKey);
                    }
                }
                try {
                    this.cache.evict(key);
                } catch (Throwable e) {
                    InfinispanEjbLogger.ROOT_LOGGER.failedToPassivateBeanGroup(e, id);
                }
            } catch (Exception e) {
                InfinispanEjbLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            }
        }
    }

    @Override
    public boolean remove(I id) {
        this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(id);
        return true;
    }

    @Override
    public BeanGroup<I, T> createGroup(I id, BeanGroupEntry<I, T> entry) {
        Mutator mutator = new CacheEntryMutator<>(this.cache, this.createKey(id), entry);
        return new InfinispanBeanGroup<>(id, entry, this.context, mutator, this);
    }
}
