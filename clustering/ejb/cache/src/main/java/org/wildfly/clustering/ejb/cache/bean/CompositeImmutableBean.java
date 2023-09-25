/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.ImmutableBean;
import org.wildfly.clustering.ejb.bean.ImmutableBeanMetaData;

/**
 * A {@link ImmutableBean} implementation composed from a {@link BeanInstance} and an {@link ImmutableBeanMetaData}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class CompositeImmutableBean<K, V extends BeanInstance<K>> implements ImmutableBean<K, V> {

    private final K id;
    private final V instance;
    private final ImmutableBeanMetaData<K> metaData;

    public CompositeImmutableBean(K id, V instance, ImmutableBeanMetaData<K> metaData) {
        this.id = id;
        this.instance = instance;
        this.metaData = metaData;
    }

    @Override
    public K getId() {
        return this.id;
    }

    @Override
    public V getInstance() {
        return this.instance;
    }

    @Override
    public ImmutableBeanMetaData<K> getMetaData() {
        return this.metaData;
    }
}
