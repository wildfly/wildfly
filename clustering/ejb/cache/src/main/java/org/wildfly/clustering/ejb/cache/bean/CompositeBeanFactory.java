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
