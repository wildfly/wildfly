/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session.metadata.fine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Unit test for {@link CompositeImmutableSessionMetaData}.
 * @author Paul Ferraro
 */
public class CompositeImmutableSessionMetaDataTestCase {
    private final ImmutableSessionCreationMetaData creationMetaData;
    private final ImmutableSessionAccessMetaData accessMetaData;

    private final ImmutableSessionMetaData metaData;

    public CompositeImmutableSessionMetaDataTestCase() {
        this(mock(ImmutableSessionCreationMetaData.class), mock(ImmutableSessionAccessMetaData.class));
    }

    private CompositeImmutableSessionMetaDataTestCase(ImmutableSessionCreationMetaData creationMetaData, ImmutableSessionAccessMetaData accessMetaData) {
        this(creationMetaData, accessMetaData, new CompositeImmutableSessionMetaData(creationMetaData, accessMetaData));
    }

    CompositeImmutableSessionMetaDataTestCase(ImmutableSessionCreationMetaData creationMetaData, ImmutableSessionAccessMetaData accessMetaData, ImmutableSessionMetaData metaData) {
        this.metaData = metaData;
        this.creationMetaData = creationMetaData;
        this.accessMetaData = accessMetaData;
    }

    @Test
    public void isNew() {
        when(this.accessMetaData.isNew()).thenReturn(true);

        assertTrue(this.metaData.isNew());

        when(this.accessMetaData.isNew()).thenReturn(false);

        assertFalse(this.metaData.isNew());
    }

    @Test
    public void isExpired() {
        when(this.creationMetaData.getCreationTime()).thenReturn(Instant.now().minus(Duration.ofMinutes(10L)));
        when(this.creationMetaData.getTimeout()).thenReturn(Duration.ofMinutes(10L));
        when(this.accessMetaData.getSinceCreationDuration()).thenReturn(Duration.ofMinutes(5L));
        when(this.accessMetaData.getLastAccessDuration()).thenReturn(Duration.ofSeconds(1));

        assertFalse(this.metaData.isExpired());

        when(this.creationMetaData.getTimeout()).thenReturn(Duration.ofMinutes(5L).minus(Duration.ofSeconds(1, 1)));

        assertTrue(this.metaData.isExpired());

        // Timeout of 0 means never expire
        when(this.creationMetaData.getTimeout()).thenReturn(Duration.ZERO);

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

        when(this.creationMetaData.getTimeout()).thenReturn(expected);

        Duration result = this.metaData.getTimeout();

        assertSame(expected, result);
    }
}
