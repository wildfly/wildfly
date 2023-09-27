/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanMetaData;

/**
 * A {@link BeanFactory} implementation that creates {@link CompositeBean} instances.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <M> the bean metadata value type
 */
public class CompositeBeanFactory<K, V extends BeanInstance<K>, M> extends CompositeImmutableBeanFactory<K, V, M> implements BeanFactory<K, V, M> {

    private final BeanMetaDataFactory<K, M> metaDataFactory;
    private final BeanGroupManager<K, V> groupManager;

    public CompositeBeanFactory(BeanMetaDataFactory<K, M> metaDataFactory, BeanGroupManager<K, V> groupManager) {
        super(metaDataFactory, groupManager);
        this.metaDataFactory = metaDataFactory;
        this.groupManager = groupManager;
    }

    @Override
    public MutableBean<K, V> createBean(K id, BeanMetaData<K> metaData, BeanGroup<K, V> group) {
        return new CompositeBean<>(id, metaData, group, this);
    }

    @Override
    public BeanMetaDataFactory<K, M> getMetaDataFactory() {
        return this.metaDataFactory;
    }

    @Override
    public BeanGroupManager<K, V> getBeanGroupManager() {
        return this.groupManager;
    }
}
