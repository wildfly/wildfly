/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.ImmutableBean;
import org.wildfly.clustering.ejb.bean.ImmutableBeanMetaData;

/**
 * A {@link ImmutableBeanFactory} implementation that creates {@link CompositeImmutableBean} instances.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <M> the bean metadata value type
 */
public class CompositeImmutableBeanFactory<K, V extends BeanInstance<K>, M> implements ImmutableBeanFactory<K, V, M> {

    private final ImmutableBeanMetaDataFactory<K, M> metaDataFactory;
    private final ImmutableBeanGroupManager<K, V> groupManager;

    public CompositeImmutableBeanFactory(ImmutableBeanMetaDataFactory<K, M> metaDataFactory, ImmutableBeanGroupManager<K, V> groupManager) {
        this.metaDataFactory = metaDataFactory;
        this.groupManager = groupManager;
    }

    @Override
    public ImmutableBeanMetaDataFactory<K, M> getMetaDataFactory() {
        return this.metaDataFactory;
    }

    @Override
    public ImmutableBeanGroupManager<K, V> getBeanGroupManager() {
        return this.groupManager;
    }

    @Override
    public ImmutableBean<K, V> createImmutableBean(K id, ImmutableBeanMetaData<K> metaData, ImmutableBeanGroup<K, V> group) {
        return new CompositeImmutableBean<>(id, group.getBeanInstance(id), metaData);
    }
}
