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
 * @author Paul Ferraro
 */
public class DefaultBeanMetaData<K> extends DefaultImmutableBeanMetaData<K> implements BeanMetaData<K> {

    private final BeanMetaDataEntry<K> entry;
    private final Runnable mutator;

    public DefaultBeanMetaData(BeanMetaDataEntry<K> entry, BeanExpiration expiration, Runnable mutator) {
        super(entry, expiration);
        this.entry = entry;
        this.mutator = mutator;
    }

    @Override
    public Instant getLastAccessTime() {
        return this.entry.getLastAccess().get();
    }

    @Override
    public void setLastAccessTime(Instant lastAccessTime) {
        Instant previousAccessTime = this.entry.getLastAccess().get();
        if (previousAccessTime.isBefore(lastAccessTime)) {
            // Retain second precision
            Duration duration = Duration.between(previousAccessTime, lastAccessTime);
            long seconds = duration.getSeconds();
            if (duration.getNano() > 0) {
                seconds += 1;
            }
            Duration normalizedDuration = (seconds > 1) ? Duration.ofSeconds(seconds) : ChronoUnit.SECONDS.getDuration();
            this.entry.getLastAccess().set(previousAccessTime.plus(normalizedDuration));
        }
    }

    @Override
    public void close() {
        this.mutator.run();
    }
}
