/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
