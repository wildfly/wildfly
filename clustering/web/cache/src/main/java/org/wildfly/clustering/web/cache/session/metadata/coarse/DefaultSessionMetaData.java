/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.web.cache.session.metadata.InvalidatableSessionMetaData;

/**
 * Default session metadata implementation that delegates to a cache entry, triggering {@link Mutator#mutate()} on close.
 * @author Paul Ferraro
 */
public class DefaultSessionMetaData extends DefaultImmutableSessionMetaData implements InvalidatableSessionMetaData {

    private final SessionMetaDataEntry entry;
    private final Mutator mutator;
    private final AtomicBoolean valid = new AtomicBoolean(true);

    public DefaultSessionMetaData(SessionMetaDataEntry entry, Mutator mutator) {
        super(entry);
        this.entry = entry;
        this.mutator = mutator;
    }

    @Override
    public boolean isValid() {
        return this.valid.get();
    }

    @Override
    public boolean invalidate() {
        return this.valid.compareAndSet(true, false);
    }

    @Override
    public void setLastAccess(Instant startTime, Instant endTime) {
        // Retain millisecond precision
        Instant normalizedStartTime = startTime.truncatedTo(ChronoUnit.MILLIS);
        // Retain second precision for last access duration
        Duration duration = Duration.between(startTime, endTime);
        long seconds = duration.getSeconds();
        if (duration.getNano() > 0) {
            seconds += 1;
        }
        Instant normalizedEndTime = normalizedStartTime.plus((seconds > 1) ? Duration.ofSeconds(seconds) : ChronoUnit.SECONDS.getDuration());

        this.entry.getLastAccessStartTime().set(normalizedStartTime);
        this.entry.getLastAccessEndTime().set(normalizedEndTime);
    }

    @Override
    public void setTimeout(Duration duration) {
        this.entry.setTimeout(duration.isNegative() ? Duration.ZERO : duration);
    }

    @Override
    public void close() {
        this.mutator.mutate();
    }
}
