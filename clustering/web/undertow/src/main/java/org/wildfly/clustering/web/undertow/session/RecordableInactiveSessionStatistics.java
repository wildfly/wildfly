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

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
    private final AtomicReference<Long> maxLifetime = new AtomicReference<>();
    // Tuple containing total lifetime of sessions, and total session count
    private final AtomicReference<Map.Entry<Long, Long>> totals = new AtomicReference<>();

    public RecordableInactiveSessionStatistics() {
        this.reset();
    }

    @Override
    public void record(ImmutableSession session) {
        long now = System.currentTimeMillis();
        long lifetime = now - session.getMetaData().getCreationTime().getTime();
        long currentMaxLifetime = this.maxLifetime.get();

        while (lifetime > currentMaxLifetime) {
            if (this.maxLifetime.compareAndSet(currentMaxLifetime, lifetime)) {
                break;
            }
            currentMaxLifetime = this.maxLifetime.get();
        }

        Map.Entry<Long, Long> currentTotals = this.totals.get();
        Map.Entry<Long, Long> sessions = createNewTotals(currentTotals, lifetime);
        while (!this.totals.compareAndSet(currentTotals, sessions)) {
            currentTotals = this.totals.get();
            sessions = createNewTotals(currentTotals, lifetime);
        }

        if (session.getMetaData().isExpired()) {
            this.expiredSessions.incrementAndGet();
        }
    }

    private static Map.Entry<Long, Long> createNewTotals(Map.Entry<Long, Long> totals, long lifetime) {
        long totalLifetime = totals.getKey();
        long totalSessions = totals.getValue();
        return new AbstractMap.SimpleImmutableEntry<>(totalLifetime + lifetime, totalSessions + 1);
    }

    @Override
    public long getMeanSessionLifetime(TimeUnit unit) {
        Map.Entry<Long, Long> totals = this.totals.get();
        long lifetime = totals.getKey();
        long count = totals.getValue();
        return (count > 0) ? unit.convert(lifetime, TimeUnit.MILLISECONDS) / count : 0;
    }

    @Override
    public long getMaxSessionLifetime(TimeUnit unit) {
        return unit.convert(this.maxLifetime.get(), TimeUnit.MILLISECONDS);
    }

    @Override
    public long getExpiredSessionCount() {
        return this.expiredSessions.get();
    }

    @Override
    public void reset() {
        this.maxLifetime.set(0L);
        this.totals.set(new AbstractMap.SimpleImmutableEntry<>(0L, 0L));
        this.expiredSessions.set(0L);
    }
}
