/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.util.Map;

import org.wildfly.clustering.ejb.bean.BeanInstance;

/**
 * A default {@link ImmutableBeanGroup} implementation based on a map of bean instances.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class DefaultImmutableBeanGroup<K, V extends BeanInstance<K>> implements ImmutableBeanGroup<K, V> {

    private final K id;
    private final Map<K, V> instances;
    private final Runnable closeTask;

    public DefaultImmutableBeanGroup(K id, Map<K, V> instances, Runnable closeTask) {
        this.id = id;
        this.instances = instances;
        this.closeTask = closeTask;
    }

    @Override
    public K getId() {
        return this.id;
    }

    @Override
    public boolean isEmpty() {
        return this.instances.isEmpty();
    }

    @Override
    public V getBeanInstance(K id) {
        return this.instances.get(id);
    }

    @Override
    public void close() {
        this.closeTask.run();
    }

    @Override
    public String toString() {
        return String.format("%s { %s -> %s }", this.getClass().getSimpleName(), this.id, this.instances.keySet());
    }
}
