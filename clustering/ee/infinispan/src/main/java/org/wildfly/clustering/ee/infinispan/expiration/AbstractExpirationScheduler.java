/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.expiration;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.expiration.ExpirationMetaData;
import org.wildfly.clustering.ee.infinispan.scheduler.AbstractCacheEntryScheduler;

/**
 * An {@link AbstractCacheEntryScheduler} suitable for expiration.
 * @author Paul Ferraro
 * @param <I> the identifier type of the scheduled object
 */
public abstract class AbstractExpirationScheduler<I> extends AbstractCacheEntryScheduler<I, ExpirationMetaData> {

    private static final Function<ExpirationMetaData, Optional<Instant>> EXPIRATION = new Function<>() {
        @Override
        public Optional<Instant> apply(ExpirationMetaData metaData) {
            return !metaData.isImmortal() ? Optional.of(metaData.getLastAccessTime().plus(metaData.getTimeout())) : Optional.empty();
        }
    };

    public AbstractExpirationScheduler(Scheduler<I, Instant> scheduler) {
        super(scheduler, EXPIRATION);
    }
}
