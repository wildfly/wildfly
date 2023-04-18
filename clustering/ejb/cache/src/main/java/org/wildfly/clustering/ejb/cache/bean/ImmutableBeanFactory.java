/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
