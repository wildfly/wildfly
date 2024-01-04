/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.ejb.bean.BeanExpiration;
import org.wildfly.clustering.ejb.bean.ImmutableBeanMetaData;

/**
 * @author Paul Ferraro
 */
public class DefaultImmutableBeanMetaData<K> implements ImmutableBeanMetaData<K> {

    private final ImmutableBeanMetaDataEntry<K> entry;
    private final BeanExpiration expiration;

    public DefaultImmutableBeanMetaData(ImmutableBeanMetaDataEntry<K> entry, BeanExpiration expiration) {
        this.entry = entry;
        this.expiration = expiration;
    }

    @Override
    public Duration getTimeout() {
        return (this.expiration != null) ? this.expiration.getTimeout() : null;
    }

    @Override
    public Instant getLastAccessTime() {
        return this.entry.getLastAccess().get();
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
