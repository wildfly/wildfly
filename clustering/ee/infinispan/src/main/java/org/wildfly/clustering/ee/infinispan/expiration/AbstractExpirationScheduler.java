/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.expiration;

import java.time.Instant;

import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.expiration.ExpirationMetaData;
import org.wildfly.clustering.ee.infinispan.scheduler.AbstractCacheEntryScheduler;

/**
 * An {@link AbstractCacheEntryScheduler} suitable for expiration.
 * @author Paul Ferraro
 * @param <I> the identifier type of the scheduled object
 */
public abstract class AbstractExpirationScheduler<I> extends AbstractCacheEntryScheduler<I, ExpirationMetaData> {

    public AbstractExpirationScheduler(Scheduler<I, Instant> scheduler) {
        super(scheduler, metaData -> !metaData.isImmortal() ? metaData.getLastAccessTime().plus(metaData.getTimeout()) : null);
    }
}
