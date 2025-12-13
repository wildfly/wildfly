/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.wildfly.clustering.ejb.bean.BeanMetaData;

/**
 * @author Paul Ferraro
 */
public class DefaultBeanMetaData<K> extends DefaultImmutableBeanMetaData<K> implements BeanMetaData<K> {

    private final BeanMetaDataEntry<K> entry;
    private final Runnable mutator;

    public DefaultBeanMetaData(BeanMetaDataEntry<K> entry, Optional<Duration> maxIdle, Runnable mutator) {
        super(entry, maxIdle);
        this.entry = entry;
        this.mutator = mutator;
    }

    @Override
    public void setLastAccessTime(Instant lastAccessTime) {
        Instant previousAccessTime = this.entry.getLastAccessTime().get();
        if (previousAccessTime.isBefore(lastAccessTime)) {
            // Retain second precision
            Duration duration = Duration.between(previousAccessTime, lastAccessTime);
            long seconds = duration.getSeconds();
            if (duration.getNano() > 0) {
                seconds += 1;
            }
            Duration normalizedDuration = (seconds > 1) ? Duration.ofSeconds(seconds) : ChronoUnit.SECONDS.getDuration();
            this.entry.getLastAccessTime().set(previousAccessTime.plus(normalizedDuration));
        }
    }

    @Override
    public void close() {
        this.mutator.run();
    }
}
