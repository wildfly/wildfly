/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache.distributable;

import java.time.Instant;
import java.util.function.Consumer;

import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBean;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.ejb.bean.Bean;
import org.wildfly.common.function.Functions;

/**
 * A distributable stateful session bean cache.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class DistributableStatefulSessionBean<K, V extends StatefulSessionBeanInstance<K>> implements StatefulSessionBean<K, V> {

    private final Bean<K, V> bean;
    private final SuspendedBatch batch;
    private final V instance;

    private volatile boolean discarded;
    private volatile boolean removed;

    public DistributableStatefulSessionBean(Bean<K, V> bean, SuspendedBatch batch) {
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
        try (Context<Batch> context = this.batch.resumeWithContext()) {
            try (Batch batch = context.get()) {
                this.bean.remove(removeTask);
            }
        }
    }

    @Override
    public void close() {
        if (this.bean.isValid()) {
            try (Context<Batch> context = this.batch.resumeWithContext()) {
                try (Batch batch = context.get()) {
                    try (Bean<K, V> bean = this.bean) {
                        this.bean.getMetaData().setLastAccessTime(Instant.now());
                    }
                }
            }
        }
    }
}
