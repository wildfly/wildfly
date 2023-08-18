/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.hotrod.bean;

import java.util.Map;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ee.hotrod.HotRodConfiguration;
import org.wildfly.clustering.ee.hotrod.RemoteCacheMutatorFactory;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.cache.bean.BeanGroupKey;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;

/**
 * Manages the cache entry for a bean group.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <C> the marshalled value context type
 */
public class HotRodBeanGroupManager<K, V extends BeanInstance<K>, C> implements Creator<K, MarshalledValue<Map<K, V>, C>, MarshalledValue<Map<K, V>, C>>, Remover<K>, MutatorFactory<K, MarshalledValue<Map<K, V>, C>> {

    private final RemoteCache<BeanGroupKey<K>, MarshalledValue<Map<K, V>, C>> cache;
    private final MutatorFactory<BeanGroupKey<K>, MarshalledValue<Map<K, V>, C>> mutatorFactory;

    public HotRodBeanGroupManager(HotRodConfiguration configuration) {
        this.cache = configuration.getCache();
        this.mutatorFactory = new RemoteCacheMutatorFactory<>(configuration.getCache());
    }

    @Override
    public MarshalledValue<Map<K, V>, C> createValue(K id, MarshalledValue<Map<K, V>, C> defaultValue) {
        BeanGroupKey<K> key = new HotRodBeanGroupKey<>(id);
        MarshalledValue<Map<K, V>, C> value = this.cache.withFlags(Flag.FORCE_RETURN_VALUE).putIfAbsent(key, defaultValue);
        return (value != null) ? value : defaultValue;
    }

    @Override
    public boolean remove(K id) {
        this.cache.remove(new HotRodBeanGroupKey<>(id));
        return true;
    }

    @Override
    public Mutator createMutator(K id, MarshalledValue<Map<K, V>, C> value) {
        return this.mutatorFactory.createMutator(new HotRodBeanGroupKey<>(id), value);
    }
}
