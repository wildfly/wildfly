/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.wildfly.clustering.cache.CacheEntryRemover;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanMetaData;

/**
 * A {@link MutableBean} implementation composed from a {@link BeanMetaData} and a {@link BeanGroup}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class CompositeBean<K, V extends BeanInstance<K>> extends CompositeImmutableBean<K, V> implements MutableBean<K, V> {

    private final BeanMetaData<K> metaData;
    private final BeanGroup<K, V> group;
    private final CacheEntryRemover<K> remover;
    private final AtomicBoolean valid = new AtomicBoolean(true);

    public CompositeBean(K id, BeanMetaData<K> metaData, BeanGroup<K, V> group, CacheEntryRemover<K> remover) {
        super(id, group.getBeanInstance(id), metaData);
        this.metaData = metaData;
        this.group = group;
        this.remover = remover;
    }

    @Override
    public BeanMetaData<K> getMetaData() {
        return this.metaData;
    }

    @Override
    public void setInstance(V instance) {
        this.group.addBeanInstance(instance);
    }

    @Override
    public boolean isValid() {
        return this.valid.get();
    }

    @Override
    public void remove(Consumer<V> removeTask) {
        // Ensure we only close group once
        if (this.valid.compareAndSet(true, false)) {
            try {
                K id = this.getId();
                V instance = this.group.removeBeanInstance(id);
                this.remover.remove(id);
                if (instance != null) {
                    removeTask.accept(instance);
                }
            } finally {
                this.group.close();
            }
        }
    }

    @Override
    public void close() {
        if (this.valid.compareAndSet(true, false)) {
            this.group.close();
        }
    }
}
