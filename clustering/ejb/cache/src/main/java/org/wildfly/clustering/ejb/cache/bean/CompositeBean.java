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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanMetaData;

/**
 * A {@link MutableBean} implementation composed from a {@link BeanMetaData} and a {@link BeanGroup}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class CompositeBean<K, V extends BeanInstance<K>> extends CompositeImmutableBean<K, V> implements MutableBean<K, V> {

    private final BeanMetaData<K> metaData;
    private final BeanGroup<K, V> group;
    private final Remover<K> remover;
    private final AtomicBoolean valid = new AtomicBoolean(true);

    public CompositeBean(K id, BeanMetaData<K> metaData, BeanGroup<K, V> group, Remover<K> remover) {
        super(id, group.getBeanInstance(id), metaData);
        this.metaData = metaData;
        this.group = group;
        this.remover = remover;
    }

    @Override
    public BeanMetaData<K> getMetaData() {
        return this.metaData;
    }

    @Override
    public void setInstance(V instance) {
        this.group.addBeanInstance(instance);
    }

    @Override
    public boolean isValid() {
        return this.valid.get();
    }

    @Override
    public void remove(Consumer<V> removeTask) {
        // Ensure we only close group once
        if (this.valid.compareAndSet(true, false)) {
            try {
                K id = this.getId();
                V instance = this.group.removeBeanInstance(id);
                this.remover.remove(id);
                if (instance != null) {
                    removeTask.accept(instance);
                }
            } finally {
                this.group.close();
            }
        }
    }

    @Override
    public void close() {
        if (this.valid.compareAndSet(true, false)) {
            this.group.close();
        }
    }
}
