/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.util.Map;
import java.util.function.Consumer;

import org.wildfly.clustering.cache.CacheEntryMutator;
import org.wildfly.clustering.ejb.bean.BeanInstance;

/**
 * A default {@link BeanGroup} implementation based on a map of bean instances.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class DefaultBeanGroup<K, V extends BeanInstance<K>> extends DefaultImmutableBeanGroup<K, V> implements MutableBeanGroup<K, V> {

    private final Map<K, V> instances;
    private final Consumer<Map<K, V>> prePassivateTask;
    private final CacheEntryMutator mutator;

    public DefaultBeanGroup(K id, Map<K, V> instances, Consumer<Map<K, V>> prePassivateTask, CacheEntryMutator mutator, Runnable closeTask) {
        super(id, instances, closeTask);
        this.instances = instances;
        this.prePassivateTask = prePassivateTask;
        this.mutator = mutator;
    }

    @Override
    public void addBeanInstance(V instance) {
        this.instances.put(instance.getId(), instance);
    }

    @Override
    public V removeBeanInstance(K id) {
        return this.instances.remove(id);
    }

    @Override
    public void run() {
        this.prePassivateTask.accept(this.instances);
        this.mutator.run();
    }
}
