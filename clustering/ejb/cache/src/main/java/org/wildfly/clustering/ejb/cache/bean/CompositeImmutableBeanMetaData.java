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
 * An {@link ImmutableBeanMetaData} implementation composed from a {@link BeanCreationMetaData} and an {@link ImmutableBeanAccessMetaData}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public class CompositeImmutableBeanMetaData<K> implements ImmutableBeanMetaData<K> {

    private final BeanCreationMetaData<K> creationMetaData;
    private final ImmutableBeanAccessMetaData accessMetaData;
    private final BeanExpiration expiration;

    public CompositeImmutableBeanMetaData(BeanCreationMetaData<K> creationMetaData, ImmutableBeanAccessMetaData accessMetaData, BeanExpiration expiration) {
        this.creationMetaData = creationMetaData;
        this.accessMetaData = accessMetaData;
        this.expiration = expiration;
    }

    @Override
    public String getName() {
        return this.creationMetaData.getName();
    }

    @Override
    public K getGroupId() {
        return this.creationMetaData.getGroupId();
    }

    @Override
    public Instant getLastAccessTime() {
        return this.creationMetaData.getCreationTime().plus(this.accessMetaData.getLastAccessDuration());
    }

    @Override
    public Duration getTimeout() {
        return (this.expiration != null) ? this.expiration.getTimeout() : null;
    }
}
