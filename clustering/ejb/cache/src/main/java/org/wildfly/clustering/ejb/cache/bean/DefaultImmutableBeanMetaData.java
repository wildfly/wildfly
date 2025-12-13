/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.wildfly.clustering.ejb.bean.ImmutableBeanMetaData;

/**
 * @author Paul Ferraro
 */
public class DefaultImmutableBeanMetaData<K> implements ImmutableBeanMetaData<K> {

    private final ImmutableBeanMetaDataEntry<K> entry;
    private final Optional<Duration> maxIdle;

    public DefaultImmutableBeanMetaData(ImmutableBeanMetaDataEntry<K> entry, Optional<Duration> maxIdle) {
        this.entry = entry;
        this.maxIdle = maxIdle;
    }

    @Override
    public Optional<Duration> getMaxIdle() {
        return this.maxIdle;
    }

    @Override
    public Optional<Instant> getLastAccessTime() {
        return Optional.of(this.entry.getLastAccessTime().get());
    }

    @Override
    public String getName() {
        return this.entry.getName();
    }

    @Override
    public K getGroupId() {
        return this.entry.getGroupId();
    }
}
