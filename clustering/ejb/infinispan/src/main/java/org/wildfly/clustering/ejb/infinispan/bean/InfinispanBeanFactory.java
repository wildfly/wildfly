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
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.context.Flag;
import org.infinispan.transaction.LockingMode;
import org.wildfly.clustering.ee.infinispan.CacheEntryMutator;
import org.wildfly.clustering.ee.infinispan.Mutator;
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
public class InfinispanBeanFactory<G, I, T> implements BeanFactory<G, I, T> {
    private final String beanName;
    private final BeanGroupFactory<G, I, T> groupFactory;
    private final Cache<BeanKey<I>, BeanEntry<G>> cache;
    private final Time timeout;
    private final PassivationListener<T> listener;

    public InfinispanBeanFactory(String beanName, BeanGroupFactory<G, I, T> groupFactory, Cache<BeanKey<I>, BeanEntry<G>> beanCache, Time timeout, PassivationListener<T> listener) {
        this.beanName = beanName;
        this.groupFactory = groupFactory;
        this.cache = beanCache;
        this.timeout = timeout;
        this.listener = listener;
    }

    @Override
    public BeanKey<I> createKey(I id) {
        return new InfinispanBeanKey<>(this.beanName, id);
    }

    @Override
    public Bean<G, I, T> createBean(I id, BeanEntry<G> entry) {
        G groupId = entry.getGroupId();
        BeanGroupEntry<I, T> groupEntry = this.groupFactory.findValue(groupId);
        if (groupEntry == null) {
             throw InfinispanEjbLogger.ROOT_LOGGER.invalidBeanGroup(id, groupId);
        }
        BeanGroup<G, I, T> group = this.groupFactory.createGroup(groupId, groupEntry);
        Mutator mutator = (entry.getLastAccessedTime() == null) ? Mutator.PASSIVE : new CacheEntryMutator<>(this.cache, this.createKey(id), entry);
        return new InfinispanBean<>(id, entry, group, mutator, this, this.timeout, this.listener);
    }

    @Override
    public BeanEntry<G> findValue(I id) {
        TransactionConfiguration transaction = this.cache.getCacheConfiguration().transaction();
        boolean pessimistic = transaction.transactionMode().isTransactional() && (transaction.lockingMode() == LockingMode.PESSIMISTIC);
        Cache<BeanKey<I>, BeanEntry<G>> cache = pessimistic ? this.cache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK) : this.cache;
        return cache.get(this.createKey(id));
    }

    @Override
    public BeanEntry<G> createValue(I id, G groupId) {
        BeanEntry<G> entry = new InfinispanBeanEntry<>(groupId);
        BeanEntry<G> existing = this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).putIfAbsent(this.createKey(id), entry);
        return (existing != null) ? existing : entry;
    }

    @Override
    public void remove(I id, RemoveListener<T> listener) {
        BeanEntry<G> entry = this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).remove(this.createKey(id));
        if (entry != null) {
            G groupId = entry.getGroupId();
            try (BeanGroup<G, I, T> group = this.groupFactory.createGroup(groupId, this.groupFactory.findValue(groupId))) {
                T bean = group.removeBean(id);
                if (listener != null) {
                    listener.removed(bean);
                }
            }
        }
    }

    @Override
    public void evict(I id) {
        BeanKey<I> key = this.createKey(id);
        BeanEntry<G> entry = this.cache.get(key);
        if (entry != null) {
            try {
                // This will trigger the @CacheEntryEvicted event in InfinispanBeanManager
                this.cache.evict(key);
            } catch (Throwable e) {
                InfinispanEjbLogger.ROOT_LOGGER.failedToPassivateBean(e, id);
            }
            // The actual bean instance is stored in the group, so this is the important entry to evict.
            this.groupFactory.evict(entry.getGroupId());
        }
    }
}
