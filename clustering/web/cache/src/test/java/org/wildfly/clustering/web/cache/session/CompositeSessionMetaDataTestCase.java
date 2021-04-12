/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.cache.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;
import org.wildfly.clustering.web.session.SessionMetaData;

public class CompositeSessionMetaDataTestCase {
    private final SessionCreationMetaData creationMetaData = mock(SessionCreationMetaData.class);
    private final SessionAccessMetaData accessMetaData = mock(SessionAccessMetaData.class);

    private final SessionMetaData metaData = new CompositeSessionMetaData(this.creationMetaData, this.accessMetaData);

    @Test
    public void isNew() {
        when(this.creationMetaData.isNew()).thenReturn(true);

        assertTrue(this.metaData.isNew());

        when(this.creationMetaData.isNew()).thenReturn(false);

        assertFalse(this.metaData.isNew());
    }

    @Test
    public void isExpired() {
        when(this.creationMetaData.getCreationTime()).thenReturn(Instant.now().minus(Duration.ofMinutes(10L)));
        when(this.creationMetaData.getMaxInactiveInterval()).thenReturn(Duration.ofMinutes(10L));
        when(this.accessMetaData.getSinceCreationDuration()).thenReturn(Duration.ofMinutes(5L));
        when(this.accessMetaData.getLastAccessDuration()).thenReturn(Duration.ofSeconds(1));

        assertFalse(this.metaData.isExpired());

        when(this.creationMetaData.getMaxInactiveInterval()).thenReturn(Duration.ofMinutes(5L).minus(Duration.ofSeconds(1, 1)));

        assertTrue(this.metaData.isExpired());

        // Max inactive interval of 0 means never expire
        when(this.creationMetaData.getMaxInactiveInterval()).thenReturn(Duration.ZERO);

        assertFalse(this.metaData.isExpired());
    }

    @Test
    public void getCreationTime() {
        Instant expected = Instant.now();

        when(this.creationMetaData.getCreationTime()).thenReturn(expected);

        Instant result = this.metaData.getCreationTime();

        assertSame(expected, result);
    }

    @Test
    public void getLastAccessStartTime() {
        Instant now = Instant.now();
        Duration sinceCreation = Duration.ofSeconds(10L);

        when(this.creationMetaData.getCreationTime()).thenReturn(now.minus(sinceCreation));
        when(this.accessMetaData.getSinceCreationDuration()).thenReturn(sinceCreation);

        Instant result = this.metaData.getLastAccessStartTime();

        assertEquals(now, result);
    }

    @Test
    public void getLastAccessEndTime() {
        Instant now = Instant.now();
        Duration sinceCreation = Duration.ofSeconds(10L);
        Duration lastAccess = Duration.ofSeconds(1L);

        when(this.creationMetaData.getCreationTime()).thenReturn(now.minus(sinceCreation).minus(lastAccess));
        when(this.accessMetaData.getSinceCreationDuration()).thenReturn(sinceCreation);
        when(this.accessMetaData.getLastAccessDuration()).thenReturn(lastAccess);

        Instant result = this.metaData.getLastAccessEndTime();

        assertEquals(now, result);
    }

    @Test
    public void getMaxInactiveInterval() {
        Duration expected = Duration.ofMinutes(30L);

        when(this.creationMetaData.getMaxInactiveInterval()).thenReturn(expected);

        Duration result = this.metaData.getMaxInactiveInterval();

        assertSame(expected, result);
    }

    @Test
    public void setLastAccessedTime() {
        // New session
        Instant endTime = Instant.now();
        Duration lastAccess = Duration.ofSeconds(1L);
        Instant startTime = endTime.minus(lastAccess);

        when(this.creationMetaData.getCreationTime()).thenReturn(startTime);

        this.metaData.setLastAccess(startTime, endTime);

        verify(this.accessMetaData).setLastAccessDuration(Duration.ZERO, lastAccess);

        reset(this.creationMetaData, this.accessMetaData);

        // Existing session
        Duration sinceCreated = Duration.ofSeconds(10L);

        when(this.creationMetaData.getCreationTime()).thenReturn(startTime.minus(sinceCreated));

        this.metaData.setLastAccess(startTime, endTime);

        verify(this.accessMetaData).setLastAccessDuration(sinceCreated, lastAccess);
    }

    @Test
    public void setMaxInactiveInterval() {
        Duration duration = Duration.ZERO;

        this.metaData.setMaxInactiveInterval(duration);

        verify(this.creationMetaData).setMaxInactiveInterval(duration);
    }
}
