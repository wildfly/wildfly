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
