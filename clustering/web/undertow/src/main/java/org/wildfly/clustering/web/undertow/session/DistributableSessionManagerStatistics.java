/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicLong;

import org.wildfly.clustering.session.ImmutableSessionMetaData;
import org.wildfly.clustering.session.SessionStatistics;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class DistributableSessionManagerStatistics implements RecordableSessionManagerStatistics {

    private final RecordableInactiveSessionStatistics inactiveSessionStatistics;
    private final SessionStatistics activeSessionStatistics;
    private final OptionalInt maxActiveSessions;
    private volatile long startTime = System.currentTimeMillis();
    private final AtomicLong createdSessionCount = new AtomicLong();
    private final AtomicLong highestSessionCount = new AtomicLong();

    public DistributableSessionManagerStatistics(SessionStatistics activeSessionStatistics, RecordableInactiveSessionStatistics inactiveSessionStatistics, OptionalInt maxActiveSessions) {
        this.activeSessionStatistics = activeSessionStatistics;
        this.inactiveSessionStatistics = inactiveSessionStatistics;
        this.maxActiveSessions = maxActiveSessions;
        this.reset();
    }

    @Override
    public Recordable<ImmutableSessionMetaData> getInactiveSessionRecorder() {
        return this.inactiveSessionStatistics;
    }

    @Override
    public void record(ImmutableSessionMetaData metaData) {
        this.createdSessionCount.incrementAndGet();
        // n.b. this is relatively expensive operation to be called on every session creation but needed to accurately report getHighestSessionCount()
        long activeCount = this.activeSessionStatistics.getActiveSessionCount();
        this.highestSessionCount.accumulateAndGet(activeCount, Math::max);
    }

    @Override
    public void reset() {
        this.createdSessionCount.set(0L);
        // n.b. reset the highest count to the current active session count - those sessions still exist after a statistics reset
        this.highestSessionCount.set(this.activeSessionStatistics.getActiveSessionCount());
        this.startTime = System.currentTimeMillis();
        this.inactiveSessionStatistics.reset();
    }

    @Override
    public long getCreatedSessionCount() {
        return this.createdSessionCount.get();
    }

    @Override
    public long getMaxActiveSessions() {
        return this.maxActiveSessions.orElse(-1);
    }

    @Override
    public long getHighestSessionCount() {
        return this.highestSessionCount.get();
    }

    @Override
    public long getActiveSessionCount() {
        return this.activeSessionStatistics.getActiveSessionCount();
    }

    @Override
    public long getExpiredSessionCount() {
        return this.inactiveSessionStatistics.getExpiredSessionCount();
    }

    @Override
    public long getRejectedSessions() {
        // We never reject sessions
        return 0;
    }

    @Override
    public long getMaxSessionAliveTime() {
        return this.inactiveSessionStatistics.getMaxSessionLifetime().toMillis();
    }

    @Override
    public long getAverageSessionAliveTime() {
        return this.inactiveSessionStatistics.getMeanSessionLifetime().toMillis();
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }
}
