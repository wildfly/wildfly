/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import java.util.Map;

import org.infinispan.Cache;
import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.ee.infinispan.CacheMutatorFactory;
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
public class InfinispanBeanGroupManager<K, V extends BeanInstance<K>, C> implements Creator<K, MarshalledValue<Map<K, V>, C>, MarshalledValue<Map<K, V>, C>>, Remover<K>, MutatorFactory<K, MarshalledValue<Map<K, V>, C>> {

    private final Cache<BeanGroupKey<K>, MarshalledValue<Map<K, V>, C>> cache;
    private final Cache<BeanGroupKey<K>, MarshalledValue<Map<K, V>, C>> removeCache;
    private final MutatorFactory<BeanGroupKey<K>, MarshalledValue<Map<K, V>, C>> mutatorFactory;

    public InfinispanBeanGroupManager(InfinispanConfiguration configuration) {
        this.cache = configuration.getCache();
        this.removeCache = configuration.getWriteOnlyCache();
        this.mutatorFactory = new CacheMutatorFactory<>(configuration.getCache());
    }

    @Override
    public MarshalledValue<Map<K, V>, C> createValue(K id, MarshalledValue<Map<K, V>, C> defaultValue) {
        MarshalledValue<Map<K, V>, C> value = this.cache.putIfAbsent(new InfinispanBeanGroupKey<>(id), defaultValue);
        return (value != null) ? value : defaultValue;
    }

    @Override
    public boolean remove(K id) {
        this.removeCache.remove(new InfinispanBeanGroupKey<>(id));
        return true;
    }

    @Override
    public Mutator createMutator(K id, MarshalledValue<Map<K, V>, C> value) {
        return this.mutatorFactory.createMutator(new InfinispanBeanGroupKey<>(id), value);
    }
}
