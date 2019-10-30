/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.undertow.session;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.InactiveSessionStatistics;

/**
 * Records statistics for inactive sessions.
 * @author Paul Ferraro
 */
public class RecordableInactiveSessionStatistics implements InactiveSessionStatistics, Recordable<ImmutableSession> {

    private final AtomicLong expiredSessions = new AtomicLong();
    private final AtomicReference<Duration> maxLifetime = new AtomicReference<>();
    // Tuple containing total lifetime of sessions, and total session count
    private final AtomicReference<Map.Entry<Duration, Long>> totals = new AtomicReference<>();

    public RecordableInactiveSessionStatistics() {
        this.reset();
    }

    @Override
    public void record(ImmutableSession session) {
        Duration lifetime = Duration.between(session.getMetaData().getCreationTime(), Instant.now());
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

        if (session.getMetaData().isExpired()) {
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
