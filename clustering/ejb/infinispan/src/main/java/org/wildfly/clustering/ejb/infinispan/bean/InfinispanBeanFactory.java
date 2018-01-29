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
package org.wildfly.clustering.ejb.infinispan.bean;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.ee.infinispan.CacheEntryMutator;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.Time;
import org.wildfly.clustering.ejb.infinispan.BeanEntry;
import org.wildfly.clustering.ejb.infinispan.BeanFactory;
import org.wildfly.clustering.ejb.infinispan.BeanGroup;
import org.wildfly.clustering.ejb.infinispan.BeanGroupEntry;
import org.wildfly.clustering.ejb.infinispan.BeanGroupFactory;
import org.wildfly.clustering.ejb.infinispan.BeanKey;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;

/**
 * Encapsulates the cache mapping strategy for a bean.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class InfinispanBeanFactory<I, T> implements BeanFactory<I, T> {
    private final String beanName;
    private final BeanGroupFactory<I, T> groupFactory;
    private final Cache<BeanKey<I>, BeanEntry<I>> cache;
    private final Cache<BeanKey<I>, BeanEntry<I>> findCache;
    private final Time timeout;
    private final PassivationListener<T> listener;

    public InfinispanBeanFactory(String beanName, BeanGroupFactory<I, T> groupFactory, Cache<BeanKey<I>, BeanEntry<I>> cache, CacheProperties properties, Time timeout, PassivationListener<T> listener) {
        this.beanName = beanName;
        this.groupFactory = groupFactory;
        this.cache = cache;
        this.findCache = properties.isLockOnRead() ? this.cache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK) : this.cache;
        this.timeout = timeout;
        this.listener = listener;
    }

    @Override
    public BeanKey<I> createKey(I id) {
        return new InfinispanBeanKey<>(id);
    }

    @Override
    public Bean<I, T> createBean(I id, BeanEntry<I> entry) {
        I groupId = entry.getGroupId();
        BeanGroupEntry<I, T> groupEntry = this.groupFactory.findValue(groupId);
        if (groupEntry == null) {
            InfinispanEjbLogger.ROOT_LOGGER.invalidBeanGroup(id, groupId);
            this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(this.createKey(id));
            return null;
        }
        BeanGroup<I, T> group = this.groupFactory.createGroup(groupId, groupEntry);
        Mutator mutator = (entry.getLastAccessedTime() == null) ? Mutator.PASSIVE : new CacheEntryMutator<>(this.cache, this.createKey(id), entry);
        return new InfinispanBean<>(id, entry, group, mutator, this, this.timeout, this.listener);
    }

    @Override
    public BeanEntry<I> findValue(I id) {
        return this.findCache.get(this.createKey(id));
    }

    @Override
    public BeanEntry<I> tryValue(I id) {
        return this.findCache.getAdvancedCache().withFlags(Flag.ZERO_LOCK_ACQUISITION_TIMEOUT, Flag.FAIL_SILENTLY).get(this.createKey(id));
    }

    @Override
    public BeanEntry<I> createValue(I id, I groupId) {
        BeanEntry<I> entry = new InfinispanBeanEntry<>(this.beanName, groupId);
        BeanEntry<I> existing = this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).putIfAbsent(this.createKey(id), entry);
        return (existing == null) ? entry : existing;
    }

    @Override
    public void remove(I id, RemoveListener<T> listener) {
        BeanEntry<I> entry = this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).remove(this.createKey(id));
        if (entry != null) {
            I groupId = entry.getGroupId();
            BeanGroupEntry<I, T> groupEntry = this.groupFactory.findValue(groupId);
            if (groupEntry != null) {
                try (BeanGroup<I, T> group = this.groupFactory.createGroup(groupId, groupEntry)) {
                    T bean = group.removeBean(id);
                    if (listener != null) {
                        listener.removed(bean);
                    }
                }
            }
        }
    }
}
