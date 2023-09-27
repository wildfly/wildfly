/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.wildfly.clustering.ejb.bean.BeanExpiration;
import org.wildfly.clustering.ejb.bean.BeanMetaData;

/**
 * A {@link BeanMetaData} implementation composed from a {@link BeanCreationMetaData} and a {@link BeanAccessMetaData}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public class CompositeBeanMetaData<K> extends CompositeImmutableBeanMetaData<K> implements BeanMetaData<K> {

    private final BeanCreationMetaData<K> creationMetaData;
    private final BeanAccessMetaData accessMetaData;

    public CompositeBeanMetaData(BeanCreationMetaData<K> creationMetaData, BeanAccessMetaData accessMetaData, BeanExpiration expiration) {
        super(creationMetaData, accessMetaData, expiration);
        this.creationMetaData = creationMetaData;
        this.accessMetaData = accessMetaData;
    }

    @Override
    public void setLastAccessTime(Instant lastAccessedTime) {
        // Only retain millisecond precision
        Duration duration = Duration.between(this.creationMetaData.getCreationTime(), lastAccessedTime);
        this.accessMetaData.setLastAccessDuration((duration.getNano() == 0) ? duration : duration.truncatedTo(ChronoUnit.MILLIS).plusMillis(1));
    }
}
