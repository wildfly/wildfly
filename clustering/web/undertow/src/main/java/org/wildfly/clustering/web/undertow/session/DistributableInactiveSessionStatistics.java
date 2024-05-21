/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.session.ImmutableSessionMetaData;

/**
 * Records statistics for inactive sessions.
 * @author Paul Ferraro
 */
public class DistributableInactiveSessionStatistics implements RecordableInactiveSessionStatistics {

    private final AtomicLong expiredSessions = new AtomicLong();
    private final AtomicReference<Duration> maxLifetime = new AtomicReference<>();
    // Tuple containing total lifetime of sessions, and total session count
    private final AtomicReference<Map.Entry<Duration, Long>> totals = new AtomicReference<>();

    public DistributableInactiveSessionStatistics() {
        this.reset();
    }

    @Override
    public void record(ImmutableSessionMetaData metaData) {
        Duration lifetime = Duration.between(metaData.getCreationTime(), Instant.now());
        Duration currentMaxLifetime = this.maxLifetime.get();

        while (lifetime.compareTo(currentMaxLifetime) > 0) {
            if (this.maxLifetime.compareAndSet(currentMaxLifetime, lifetime)) {
                break;
            }
            currentMaxLifetime = this.maxLifetime.get();
        }

        Map.Entry<Duration, Long> currentTotals = this.totals.get();
        Map.Entry<Duration, Long> sessions = createNewTotals(currentTotals, lifetime);
        while (!this.totals.compareAndSet(currentTotals, sessions)) {
            currentTotals = this.totals.get();
            sessions = createNewTotals(currentTotals, lifetime);
        }

        if (metaData.isExpired()) {
            this.expiredSessions.incrementAndGet();
        }
    }

    private static Map.Entry<Duration, Long> createNewTotals(Map.Entry<Duration, Long> totals, Duration lifetime) {
        Duration totalLifetime = totals.getKey();
        long totalSessions = totals.getValue();
        return new AbstractMap.SimpleImmutableEntry<>(totalLifetime.plus(lifetime), totalSessions + 1);
    }

    @Override
    public Duration getMeanSessionLifetime() {
        Map.Entry<Duration, Long> totals = this.totals.get();
        Duration lifetime = totals.getKey();
        long count = totals.getValue();
        return (count > 0) ? lifetime.dividedBy(count) : Duration.ZERO;
    }

    @Override
    public Duration getMaxSessionLifetime() {
        return this.maxLifetime.get();
    }

    @Override
    public long getExpiredSessionCount() {
        return this.expiredSessions.get();
    }

    @Override
    public void reset() {
        this.maxLifetime.set(Duration.ZERO);
        this.totals.set(new AbstractMap.SimpleImmutableEntry<>(Duration.ZERO, 0L));
        this.expiredSessions.set(0L);
    }
}
