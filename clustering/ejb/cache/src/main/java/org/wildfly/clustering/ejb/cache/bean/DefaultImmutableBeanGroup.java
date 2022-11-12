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

import java.util.Map;

import org.wildfly.clustering.ejb.bean.BeanInstance;

/**
 * A default {@link ImmutableBeanGroup} implementation based on a map of bean instances.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class DefaultImmutableBeanGroup<K, V extends BeanInstance<K>> implements ImmutableBeanGroup<K, V> {

    private final K id;
    private final Map<K, V> instances;
    private final Runnable closeTask;

    public DefaultImmutableBeanGroup(K id, Map<K, V> instances, Runnable closeTask) {
        this.id = id;
        this.instances = instances;
        this.closeTask = closeTask;
    }

    @Override
    public K getId() {
        return this.id;
    }

    @Override
    public boolean isEmpty() {
        return this.instances.isEmpty();
    }

    @Override
    public V getBeanInstance(K id) {
        return this.instances.get(id);
    }

    @Override
    public void close() {
        this.closeTask.run();
    }

    @Override
    public String toString() {
        return String.format("%s { %s -> %s }", this.getClass().getSimpleName(), this.id, this.instances.keySet());
    }
}
