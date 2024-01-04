/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ee.Locator;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.ImmutableBean;
import org.wildfly.clustering.ejb.bean.ImmutableBeanMetaData;

/**
 * A factory for creating {@link ImmutableBean} instances from an {@link ImmutableBeanMetaDataFactory} and an {@link ImmutableBeanGroupManager}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <M> the bean metadata value type
 */
public interface ImmutableBeanFactory<K, V extends BeanInstance<K>, M> extends Locator<K, M> {
    ImmutableBeanMetaDataFactory<K, M> getMetaDataFactory();
    ImmutableBeanGroupManager<K, V> getBeanGroupManager();

    @Override
    default M findValue(K id) {
        return this.getMetaDataFactory().findValue(id);
    }

    @Override
    default M tryValue(K id) {
        return this.getMetaDataFactory().tryValue(id);
    }

    default ImmutableBean<K, V> createImmutableBean(K id, M value) {
        ImmutableBeanMetaData<K> metaData = this.getMetaDataFactory().createImmutableBeanMetaData(id, value);
        ImmutableBeanGroup<K, V> group = this.getBeanGroupManager().getImmutableBeanGroup(metaData.getGroupId());
        return this.createImmutableBean(id, metaData, group);
    }

    ImmutableBean<K, V> createImmutableBean(K id, ImmutableBeanMetaData<K> metaData, ImmutableBeanGroup<K, V> group);
}
