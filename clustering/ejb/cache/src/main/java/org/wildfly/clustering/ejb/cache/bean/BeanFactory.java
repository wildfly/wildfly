/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanMetaData;

/**
 * A factory for creating {@link MutableBean} instances from a {@link BeanMetaDataFactory} and a {@link BeanGroupManager}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <M> the bean metadata value type
 */
public interface BeanFactory<K, V extends BeanInstance<K>, M> extends ImmutableBeanFactory<K, V, M>, Creator<V, M, K>, Remover<K> {

    @Override
    BeanMetaDataFactory<K, M> getMetaDataFactory();
    @Override
    BeanGroupManager<K, V> getBeanGroupManager();

    @Override
    default M createValue(V id, K groupId) {
        return this.getMetaDataFactory().createValue(id, groupId);
    }

    @Override
    default boolean remove(K id) {
        return this.getMetaDataFactory().remove(id);
    }

    @Override
    default boolean purge(K id) {
        return this.getMetaDataFactory().purge(id);
    }

    default MutableBean<K, V> createBean(K id, M value) {
        BeanMetaData<K> metaData = this.getMetaDataFactory().createBeanMetaData(id, value);
        BeanGroup<K, V> group = this.getBeanGroupManager().getBeanGroup(metaData.getGroupId());
        return this.createBean(id, metaData, group);
    }

    MutableBean<K, V> createBean(K id, BeanMetaData<K> metaData, BeanGroup<K, V> group);
}
