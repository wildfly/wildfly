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
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.Creator;
import org.jboss.as.clustering.infinispan.invoker.Locator;
import org.jboss.as.clustering.infinispan.invoker.Mutator;
import org.jboss.as.clustering.infinispan.invoker.Remover.RemoveOperation;
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
    private final CacheInvoker invoker;
    private final Time timeout;
    private final PassivationListener<T> listener;

    public InfinispanBeanFactory(String beanName, BeanGroupFactory<G, I, T> groupFactory, Cache<BeanKey<I>, BeanEntry<G>> beanCache, CacheInvoker invoker, Time timeout, PassivationListener<T> listener) {
        this.beanName = beanName;
        this.groupFactory = groupFactory;
        this.cache = beanCache;
        this.invoker = invoker;
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
        Mutator mutator = (entry.getLastAccessedTime() == null) ? Mutator.PASSIVE : new BeanMutator<>(this.invoker, this.cache, this.createKey(id), entry);
        return new InfinispanBean<>(id, entry, group, mutator, this, this.timeout, this.listener);
    }

    @Override
    public BeanEntry<G> findValue(I id) {
        return this.invoker.invoke(this.cache, new LockingFindOperation<BeanKey<I>, BeanEntry<G>>(this.createKey(id)));
    }

    @Override
    public BeanEntry<G> createValue(I id, G groupId) {
        BeanEntry<G> entry = new InfinispanBeanEntry<>(groupId);
        BeanEntry<G> existing = this.invoker.invoke(this.cache, new Creator.CreateOperation<>(this.createKey(id), entry), Flag.FORCE_SYNCHRONOUS);
        return (existing != null) ? existing : entry;
    }

    @Override
    public void remove(I id, RemoveListener<T> listener) {
        BeanEntry<G> entry = this.invoker.invoke(this.cache, new RemoveOperation<BeanKey<I>, BeanEntry<G>>(this.createKey(id)), Flag.FORCE_SYNCHRONOUS);
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
        BeanEntry<G> entry = this.invoker.invoke(this.cache, new Locator.FindOperation<BeanKey<I>, BeanEntry<G>>(key));
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

    private static class BeanMutator<G, I> implements Mutator {
        private final CacheInvoker invoker;
        private final Cache<BeanKey<I>, BeanEntry<G>> cache;
        private final BeanKey<I> key;
        private final BeanEntry<G> entry;

        BeanMutator(CacheInvoker invoker, Cache<BeanKey<I>, BeanEntry<G>> cache, BeanKey<I> key, BeanEntry<G> entry) {
            this.invoker = invoker;
            this.cache = cache;
            this.key = key;
            this.entry = entry;
        }

        @Override
        public void mutate() {
            this.invoker.invoke(this.cache, new MutateOperation<>(this.key, this.entry), Flag.IGNORE_RETURN_VALUES);
        }
    }
}
