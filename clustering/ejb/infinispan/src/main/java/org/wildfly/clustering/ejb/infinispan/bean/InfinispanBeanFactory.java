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

import java.time.Duration;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.infinispan.InfinispanMutatorFactory;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.PassivationListener;
import org.wildfly.clustering.ejb.RemoveListener;
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
public class InfinispanBeanFactory<I, T, C> implements BeanFactory<I, T> {
    private final String beanName;
    private final BeanGroupFactory<I, T, C> groupFactory;
    private final Cache<BeanKey<I>, BeanEntry<I>> cache;
    private final Cache<BeanKey<I>, BeanEntry<I>> findCache;
    private final Duration timeout;
    private final PassivationListener<T> listener;
    private final MutatorFactory<BeanKey<I>, BeanEntry<I>> mutatorFactory;

    public InfinispanBeanFactory(String beanName, BeanGroupFactory<I, T, C> groupFactory, Cache<BeanKey<I>, BeanEntry<I>> cache, CacheProperties properties, Duration timeout, PassivationListener<T> listener) {
        this.beanName = beanName;
        this.groupFactory = groupFactory;
        this.cache = cache;
        this.findCache = properties.isLockOnRead() ? this.cache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK) : this.cache;
        this.timeout = timeout;
        this.listener = listener;
        this.mutatorFactory = new InfinispanMutatorFactory<>(cache, properties);
    }

    @Override
    public BeanKey<I> createKey(I id) {
        return new InfinispanBeanKey<>(id);
    }

    @Override
    public Bean<I, T> createBean(I id, BeanEntry<I> entry) {
        I groupId = entry.getGroupId();
        BeanGroupEntry<I, T, C> groupEntry = this.groupFactory.findValue(groupId);
        if (groupEntry == null) {
            InfinispanEjbLogger.ROOT_LOGGER.invalidBeanGroup(id, groupId);
            this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(this.createKey(id));
            return null;
        }
        BeanGroup<I, T> group = this.groupFactory.createGroup(groupId, groupEntry);
        Mutator mutator = (entry.getLastAccessedTime() == null) ? Mutator.PASSIVE : this.mutatorFactory.createMutator(this.createKey(id), entry);
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
        this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).put(this.createKey(id), entry);
        return entry;
    }

    @Override
    public boolean remove(I id, RemoveListener<T> listener) {
        BeanEntry<I> entry = this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).remove(this.createKey(id));
        if (entry != null) {
            I groupId = entry.getGroupId();
            BeanGroupEntry<I, T, C> groupEntry = this.groupFactory.findValue(groupId);
            if (groupEntry != null) {
                try (BeanGroup<I, T> group = this.groupFactory.createGroup(groupId, groupEntry)) {
                    T bean = group.removeBean(id, this.listener);
                    if (listener != null) {
                        listener.removed(bean);
                    }
                }
            }
        }
        return true;
    }
}
