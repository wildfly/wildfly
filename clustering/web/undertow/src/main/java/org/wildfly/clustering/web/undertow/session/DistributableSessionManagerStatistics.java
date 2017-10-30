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

import java.util.concurrent.atomic.AtomicLong;

import org.wildfly.clustering.web.session.ActiveSessionStatistics;
import org.wildfly.clustering.web.session.InactiveSessionStatistics;

import io.undertow.server.session.Session;

/**
 * @author Paul Ferraro
 */
public class DistributableSessionManagerStatistics implements RecordableSessionManagerStatistics {

    private final InactiveSessionStatistics inactiveSessionStatistics;
    private final ActiveSessionStatistics activeSessionStatistics;
    private final int maxActiveSessions;
    private volatile long startTime = System.currentTimeMillis();
    private final AtomicLong createdSessionCount = new AtomicLong();

    public DistributableSessionManagerStatistics(ActiveSessionStatistics activeSessionStatistics, InactiveSessionStatistics inactiveSessionStatistics, int maxActiveSessions) {
        this.activeSessionStatistics = activeSessionStatistics;
        this.inactiveSessionStatistics = inactiveSessionStatistics;
        this.maxActiveSessions = maxActiveSessions;
        this.reset();
    }

    @Override
    public void record(Session object) {
        this.createdSessionCount.incrementAndGet();
    }

    @Override
    public void reset() {
        this.createdSessionCount.set(0L);
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public long getCreatedSessionCount() {
        return this.createdSessionCount.get();
    }

    @Override
    public long getMaxActiveSessions() {
        return this.maxActiveSessions;
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
