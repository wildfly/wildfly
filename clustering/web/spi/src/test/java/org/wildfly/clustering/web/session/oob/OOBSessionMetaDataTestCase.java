/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.session.oob;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Unit test for {@link OOBSessionMetaData}.
 * @author Paul Ferraro
 */
public class OOBSessionMetaDataTestCase {
    private final SessionManager<Object, Batch> manager = mock(SessionManager.class);
    private final String id = "ABC123";

    private final SessionMetaData metaData = new OOBSessionMetaData<>(this.manager, this.id);

    @Test
    public void isNew() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, this.metaData::isNew);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        boolean expected = true;

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.isNew()).thenReturn(expected);

        boolean result = this.metaData.isNew();

        Assert.assertEquals(expected, result);

        verify(batch).close();
    }

    @Test
    public void isExpired() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, this.metaData::isExpired);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        boolean expected = true;

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.isExpired()).thenReturn(expected);

        boolean result = this.metaData.isExpired();

        Assert.assertEquals(expected, result);

        verify(batch).close();
    }

    @Test
    public void getCreationTime() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, this.metaData::getCreationTime);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        Instant expected = Instant.now();

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getCreationTime()).thenReturn(expected);

        Instant result = this.metaData.getCreationTime();

        Assert.assertSame(expected, result);

        verify(batch).close();
    }

    @Test
    public void getLastAccessStartTime() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, this.metaData::getLastAccessStartTime);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        Instant expected = Instant.now();

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getLastAccessStartTime()).thenReturn(expected);

        Instant result = this.metaData.getLastAccessStartTime();

        Assert.assertSame(expected, result);

        verify(batch).close();
    }

    @Test
    public void getLastAccessEndTime() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, this.metaData::getLastAccessEndTime);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        Instant expected = Instant.now();

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getLastAccessEndTime()).thenReturn(expected);

        Instant result = this.metaData.getLastAccessEndTime();

        Assert.assertSame(expected, result);

        verify(batch).close();
    }

    @Test
    public void getMaxInactiveInterval() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, this.metaData::getMaxInactiveInterval);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        Duration expected = Duration.ZERO;

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getMaxInactiveInterval()).thenReturn(expected);

        Duration result = this.metaData.getMaxInactiveInterval();

        Assert.assertSame(expected, result);

        verify(batch).close();
    }

    @Test
    public void setLastAccess() {
        Assert.assertThrows(IllegalStateException.class, () -> this.metaData.setLastAccess(Instant.now(), Instant.now()));
    }

    @Test
    public void setMaxInactiveInterval() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        Duration duration = Duration.ZERO;

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.findSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, () -> this.metaData.setMaxInactiveInterval(duration));

        verify(batch).close();
        reset(batch);

        Session<Object> session = mock(Session.class);
        SessionMetaData metaData = mock(SessionMetaData.class);

        when(this.manager.findSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);

        this.metaData.setMaxInactiveInterval(duration);

        verify(metaData).setMaxInactiveInterval(duration);
        verify(session).close();
        verify(batch).close();
    }
}
