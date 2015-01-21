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

import java.util.HashMap;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.infinispan.CacheEntryMutator;
import org.wildfly.clustering.ee.infinispan.Mutator;
import org.wildfly.clustering.ejb.infinispan.BeanGroup;
import org.wildfly.clustering.ejb.infinispan.BeanGroupEntry;
import org.wildfly.clustering.ejb.infinispan.BeanGroupFactory;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.marshalling.MarshalledValueFactory;
import org.wildfly.clustering.marshalling.MarshallingContext;

/**
 * Encapsulates the cache mapping strategy of a bean group.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class InfinispanBeanGroupFactory<G, I, T> implements BeanGroupFactory<G, I, T> {

    private final Cache<G, BeanGroupEntry<I, T>> cache;
    private final MarshalledValueFactory<MarshallingContext> factory;
    private final MarshallingContext context;

    public InfinispanBeanGroupFactory(Cache<G, BeanGroupEntry<I, T>> cache, MarshalledValueFactory<MarshallingContext> factory, MarshallingContext context) {
        this.cache = cache;
        this.factory = factory;
        this.context = context;
    }

    @Override
    public BeanGroupEntry<I, T> createValue(G id, Void context) {
        Map<I, T> beans = new HashMap<>();
        BeanGroupEntry<I, T> entry = new InfinispanBeanGroupEntry<>(this.factory.createMarshalledValue(beans));
        BeanGroupEntry<I, T> existing = this.cache.getAdvancedCache().withFlags(Flag.FORCE_SYNCHRONOUS).putIfAbsent(id, entry);
        return (existing != null) ? existing : entry;
    }

    @Override
    public BeanGroupEntry<I, T> findValue(G id) {
        return this.cache.get(id);
    }

    @Override
    public void evict(G id) {
        try {
            this.cache.evict(id);
        } catch (Throwable e) {
            InfinispanEjbLogger.ROOT_LOGGER.failedToPassivateBeanGroup(e, id);
        }
    }

    @Override
    public void remove(G id) {
        this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(id);
    }

    @Override
    public BeanGroup<G, I, T> createGroup(final G id, final BeanGroupEntry<I, T> entry) {
        Mutator mutator = new CacheEntryMutator<>(this.cache, id, entry);
        return new InfinispanBeanGroup<>(id, entry, this.context, mutator, this);
    }
}
