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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.undertow.session.RecordableInactiveSessionStatistics;

/**
 * @author Paul Ferraro
 */
public class RecordableInactiveSessionStatisticsTestCase {
    private final RecordableInactiveSessionStatistics statistics = new RecordableInactiveSessionStatistics();

    @Test
    public void test() {
        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        Date now = new Date();
        Date created = new Date(now.getTime() - TimeUnit.MINUTES.toMillis(20));

        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.isExpired()).thenReturn(false);
        when(metaData.getCreationTime()).thenReturn(created);

        this.statistics.record(session);

        assertEquals(0L, this.statistics.getExpiredSessionCount());
        assertEquals(20L, this.statistics.getMaxSessionLifetime(TimeUnit.MINUTES));
        assertEquals(20L, this.statistics.getMeanSessionLifetime(TimeUnit.MINUTES));

        now = new Date();
        created = new Date(now.getTime() - TimeUnit.MINUTES.toMillis(10));

        when(metaData.isExpired()).thenReturn(true);
        when(metaData.getCreationTime()).thenReturn(created);

        this.statistics.record(session);

        assertEquals(1L, this.statistics.getExpiredSessionCount());
        assertEquals(20L, this.statistics.getMaxSessionLifetime(TimeUnit.MINUTES));
        assertEquals(15L, this.statistics.getMeanSessionLifetime(TimeUnit.MINUTES));

        this.statistics.reset();

        assertEquals(0L, this.statistics.getExpiredSessionCount());
        assertEquals(0L, this.statistics.getMaxSessionLifetime(TimeUnit.MINUTES));
        assertEquals(0L, this.statistics.getMeanSessionLifetime(TimeUnit.MINUTES));
    }
}
