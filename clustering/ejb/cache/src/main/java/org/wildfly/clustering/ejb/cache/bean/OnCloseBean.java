/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.util.function.Consumer;

import org.wildfly.clustering.ejb.bean.Bean;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanMetaData;

/**
 * A bean decorator that executes a given task on close.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class OnCloseBean<K, V extends BeanInstance<K>> implements Bean<K, V> {

    private final Bean<K, V> bean;
    private final Consumer<Bean<K, V>> task;

    public OnCloseBean(Bean<K, V> bean, Consumer<Bean<K, V>> task) {
        this.bean = bean;
        this.task = task;
    }

    @Override
    public K getId() {
        return this.bean.getId();
    }

    @Override
    public V getInstance() {
        return this.bean.getInstance();
    }

    @Override
    public BeanMetaData<K> getMetaData() {
        return this.bean.getMetaData();
    }

    @Override
    public boolean isValid() {
        return this.bean.isValid();
    }

    @Override
    public void remove(Consumer<V> removeTask) {
        this.bean.remove(removeTask);
    }

    @Override
    public void close() {
        try {
            this.task.accept(this.bean);
        } finally {
            this.bean.close();
        }
    }
}