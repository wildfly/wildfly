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
