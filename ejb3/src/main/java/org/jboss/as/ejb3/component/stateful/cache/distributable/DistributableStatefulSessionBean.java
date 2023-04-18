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

package org.jboss.as.ejb3.component.stateful.cache.distributable;

import java.time.Instant;
import java.util.function.Consumer;

import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBean;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ejb.bean.Bean;
import org.wildfly.common.function.Functions;

/**
 * A distributable stateful session bean cache.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class DistributableStatefulSessionBean<K, V extends StatefulSessionBeanInstance<K>> implements StatefulSessionBean<K, V> {

    private final Batcher<Batch> batcher;
    private final Bean<K, V> bean;
    private final Batch batch;
    private final V instance;

    private volatile boolean discarded;
    private volatile boolean removed;

    public DistributableStatefulSessionBean(Batcher<Batch> batcher, Bean<K, V> bean, Batch batch) {
        this.batcher = batcher;
        this.bean = bean;
        // Store reference to bean instance eagerly, so that it is accessible even after removal
        this.instance = bean.getInstance();
        this.batch = batch;
    }

    @Override
    public K getId() {
        return this.bean.getId();
    }

    @Override
    public V getInstance() {
        return this.instance;
    }

    @Override
    public boolean isClosed() {
        return !this.bean.isValid();
    }

    @Override
    public boolean isDiscarded() {
        return this.discarded;
    }

    @Override
    public boolean isRemoved() {
        return this.removed;
    }

    @Override
    public void discard() {
        if (this.bean.isValid()) {
            this.remove(Functions.discardingConsumer());
            this.discarded = true;
        }
    }

    @Override
    public void remove() {
        if (this.bean.isValid()) {
            this.remove(StatefulSessionBeanInstance::removed);
            this.removed = true;
        }
    }

    private void remove(Consumer<V> removeTask) {
        try (BatchContext context = this.batcher.resumeBatch(this.batch)) {
            try {
                this.bean.remove(removeTask);
            } finally {
                this.batch.close();
            }
        }
    }

    @Override
    public void close() {
        if (this.bean.isValid()) {
            try (BatchContext context = this.batcher.resumeBatch(this.batch)) {
                this.bean.getMetaData().setLastAccessTime(Instant.now());
                this.bean.close();
            } finally {
                this.batch.close();
            }
        }
    }
}
