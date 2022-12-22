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

import java.util.function.Consumer;

import org.wildfly.clustering.ejb.bean.Bean;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.ejb.bean.BeanMetaData;

/**
 * A bean decorator that executes a given task on close.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class OnCloseBean<K, V extends BeanInstance<K>> implements Bean<K, V> {

    private final Bean<K, V> bean;
    private final Consumer<Bean<K, V>> task;

    public OnCloseBean(Bean<K, V> bean, Consumer<Bean<K, V>> task) {
        this.bean = bean;
        this.task = task;
    }

    @Override
    public K getId() {
        return this.bean.getId();
    }

    @Override
    public V getInstance() {
        return this.bean.getInstance();
    }

    @Override
    public BeanMetaData<K> getMetaData() {
        return this.bean.getMetaData();
    }

    @Override
    public boolean isValid() {
        return this.bean.isValid();
    }

    @Override
    public void remove(Consumer<V> removeTask) {
        this.bean.remove(removeTask);
    }

    @Override
    public void close() {
        try {
            this.task.accept(this.bean);
        } finally {
            this.bean.close();
        }
    }
}