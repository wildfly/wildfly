/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.OptionalInt;

import org.junit.Test;
import org.wildfly.clustering.session.ImmutableSessionMetaData;
import org.wildfly.clustering.session.SessionStatistics;

/**
 * Unit test for {@link DistributableSessionManagerStatistics}.
 *
 * @author Radoslav Husar
 */
public class DistributableSessionManagerStatisticsTestCase {

    @Test
    public void highestSessionCount() {
        SessionStatistics activeSessionStatistics = mock(SessionStatistics.class);
        RecordableInactiveSessionStatistics inactiveSessionStatistics = mock(RecordableInactiveSessionStatistics.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);

        // Initial state: 0 active sessions
        when(activeSessionStatistics.getActiveSessionCount()).thenReturn(0L);

        RecordableSessionManagerStatistics statistics = new DistributableSessionManagerStatistics(activeSessionStatistics, inactiveSessionStatistics, OptionalInt.empty());

        // After construction with 0 active sessions, highest should be 0
        assertEquals(0L, statistics.getHighestSessionCount());

        // Simulate first session created, now 1 active session
        when(activeSessionStatistics.getActiveSessionCount()).thenReturn(1L);
        statistics.record(metaData);
        assertEquals(1L, statistics.getHighestSessionCount());

        // Simulate second session created, now 2 active sessions
        when(activeSessionStatistics.getActiveSessionCount()).thenReturn(2L);
        statistics.record(metaData);
        assertEquals(2L, statistics.getHighestSessionCount());

        // Simulate third session created, now 3 active sessions
        when(activeSessionStatistics.getActiveSessionCount()).thenReturn(3L);
        statistics.record(metaData);
        assertEquals(3L, statistics.getHighestSessionCount());

        // Simulate session expired (active count drops), highest should remain at 3
        when(activeSessionStatistics.getActiveSessionCount()).thenReturn(2L);
        // No record() call since session expired, not created
        assertEquals(3L, statistics.getHighestSessionCount());

        // Reset with 2 active sessions - highest should reset to current active count
        statistics.reset();
        assertEquals(2L, statistics.getHighestSessionCount());

        // New session after reset
        when(activeSessionStatistics.getActiveSessionCount()).thenReturn(3L);
        statistics.record(metaData);
        assertEquals(3L, statistics.getHighestSessionCount());
    }

    @Test
    public void createdSessionCount() {
        SessionStatistics activeSessionStatistics = mock(SessionStatistics.class);
        RecordableInactiveSessionStatistics inactiveSessionStatistics = mock(RecordableInactiveSessionStatistics.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);

        when(activeSessionStatistics.getActiveSessionCount()).thenReturn(0L);

        RecordableSessionManagerStatistics statistics = new DistributableSessionManagerStatistics(activeSessionStatistics, inactiveSessionStatistics, OptionalInt.empty());

        assertEquals(0L, statistics.getCreatedSessionCount());

        statistics.record(metaData);
        assertEquals(1L, statistics.getCreatedSessionCount());

        statistics.record(metaData);
        assertEquals(2L, statistics.getCreatedSessionCount());

        statistics.reset();
        assertEquals(0L, statistics.getCreatedSessionCount());
    }

    @Test
    public void maxActiveSessions() {
        SessionStatistics activeSessionStatistics = mock(SessionStatistics.class);
        RecordableInactiveSessionStatistics inactiveSessionStatistics = mock(RecordableInactiveSessionStatistics.class);

        when(activeSessionStatistics.getActiveSessionCount()).thenReturn(0L);

        // Test with no limit
        RecordableSessionManagerStatistics statistics = new DistributableSessionManagerStatistics(activeSessionStatistics, inactiveSessionStatistics, OptionalInt.empty());
        assertEquals(-1L, statistics.getMaxActiveSessions());

        // Test with limit
        statistics = new DistributableSessionManagerStatistics(activeSessionStatistics, inactiveSessionStatistics, OptionalInt.of(100));
        assertEquals(100L, statistics.getMaxActiveSessions());
    }
}
