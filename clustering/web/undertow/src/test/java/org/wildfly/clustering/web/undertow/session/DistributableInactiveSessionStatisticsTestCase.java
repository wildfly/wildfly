/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;
import org.wildfly.clustering.session.ImmutableSessionMetaData;

/**
 * Unit test for {@link DistributableInactiveSessionStatistics}.
 *
 * @author Paul Ferraro
 */
public class DistributableInactiveSessionStatisticsTestCase {
    private final RecordableInactiveSessionStatistics statistics = new DistributableInactiveSessionStatistics();

    @Test
    public void test() {
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        Instant now = Instant.now();
        Instant created = now.minus(Duration.ofMinutes(20L));

        when(metaData.isExpired()).thenReturn(false);
        when(metaData.getCreationTime()).thenReturn(created);

        this.statistics.record(metaData);

        assertEquals(0L, this.statistics.getExpiredSessionCount());
        assertEquals(20L, this.statistics.getMaxSessionLifetime().toMinutes());
        assertEquals(20L, this.statistics.getMeanSessionLifetime().toMinutes());

        now = Instant.now();
        created = now.minus(Duration.ofMinutes(10L));

        when(metaData.isExpired()).thenReturn(true);
        when(metaData.getCreationTime()).thenReturn(created);

        this.statistics.record(metaData);

        assertEquals(1L, this.statistics.getExpiredSessionCount());
        assertEquals(20L, this.statistics.getMaxSessionLifetime().toMinutes());
        assertEquals(15L, this.statistics.getMeanSessionLifetime().toMinutes());

        this.statistics.reset();

        assertEquals(0L, this.statistics.getExpiredSessionCount());
        assertEquals(0L, this.statistics.getMaxSessionLifetime().toMinutes());
        assertEquals(0L, this.statistics.getMeanSessionLifetime().toMinutes());
    }
}
