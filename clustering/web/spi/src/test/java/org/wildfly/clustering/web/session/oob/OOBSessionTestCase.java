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

import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Unit test for {@link OOBSession}.
 * @author Paul Ferraro
 */
public class OOBSessionTestCase {
    private final SessionManager<Object, Batch> manager = mock(SessionManager.class);
    private final String id = "ABC123";
    private final Object localContext = new Object();

    private final Session<Object> session = new OOBSession<>(this.manager, this.id, this.localContext);

    @Test
    public void getId() {
        Assert.assertSame(this.id, this.session.getId());
    }

    @Test
    public void isValid() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(this.id)).thenReturn(null);

        Assert.assertFalse(this.session.isValid());

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);

        when(this.manager.readSession(this.id)).thenReturn(session);

        Assert.assertTrue(this.session.isValid());

        verify(batch).close();
    }

    @Test
    public void getMetaData() {
        SessionMetaData metaData = this.session.getMetaData();
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, metaData::getCreationTime);

        verify(batch).close();
    }

    @Test
    public void getAttributes() {
        SessionAttributes attributes = this.session.getAttributes();
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, attributes::getAttributeNames);

        verify(batch).close();
    }

    @Test
    public void invalidate() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.findSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, this.session::invalidate);

        verify(batch).close();
        reset(batch);

        Session<Object> session = mock(Session.class);

        when(this.manager.findSession(this.id)).thenReturn(session);

        this.session.invalidate();

        verify(session).invalidate();
        verify(session).close();
        verify(batch).close();
    }


    @Test
    public void isNew() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, this.session.getMetaData()::isNew);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        boolean expected = true;

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.isNew()).thenReturn(expected);

        boolean result = this.session.getMetaData().isNew();

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

        Assert.assertThrows(IllegalStateException.class, this.session.getMetaData()::isExpired);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        boolean expected = true;

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.isExpired()).thenReturn(expected);

        boolean result = this.session.getMetaData().isExpired();

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

        Assert.assertThrows(IllegalStateException.class, this.session.getMetaData()::getCreationTime);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        Instant expected = Instant.now();

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getCreationTime()).thenReturn(expected);

        Instant result = this.session.getMetaData().getCreationTime();

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

        Assert.assertThrows(IllegalStateException.class, this.session.getMetaData()::getLastAccessStartTime);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        Instant expected = Instant.now();

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getLastAccessStartTime()).thenReturn(expected);

        Instant result = this.session.getMetaData().getLastAccessStartTime();

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

        Assert.assertThrows(IllegalStateException.class, this.session.getMetaData()::getLastAccessEndTime);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        Instant expected = Instant.now();

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getLastAccessEndTime()).thenReturn(expected);

        Instant result = this.session.getMetaData().getLastAccessEndTime();

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

        Assert.assertThrows(IllegalStateException.class, this.session.getMetaData()::getMaxInactiveInterval);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        Duration expected = Duration.ZERO;

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getMaxInactiveInterval()).thenReturn(expected);

        Duration result = this.session.getMetaData().getMaxInactiveInterval();

        Assert.assertSame(expected, result);

        verify(batch).close();
    }

    @Test
    public void setLastAccess() {
        Assert.assertThrows(IllegalStateException.class, () -> this.session.getMetaData().setLastAccess(Instant.now(), Instant.now()));
    }

    @Test
    public void setMaxInactiveInterval() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        Duration duration = Duration.ZERO;

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.findSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, () -> this.session.getMetaData().setMaxInactiveInterval(duration));

        verify(batch).close();
        reset(batch);

        Session<Object> session = mock(Session.class);
        SessionMetaData metaData = mock(SessionMetaData.class);

        when(this.manager.findSession(this.id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);

        this.session.getMetaData().setMaxInactiveInterval(duration);

        verify(metaData).setMaxInactiveInterval(duration);
        verify(session).close();
        verify(batch).close();
    }

    @Test
    public void getAttributeNames() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, this.session.getAttributes()::getAttributeNames);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionAttributes attributes = mock(ImmutableSessionAttributes.class);
        Set<String> expected = Collections.singleton("foo");

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttributeNames()).thenReturn(expected);

        Set<String> result = this.session.getAttributes().getAttributeNames();

        Assert.assertSame(expected, result);

        verify(batch).close();
    }

    @Test
    public void getAttribute() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        String attributeName = "foo";

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, () -> this.session.getAttributes().getAttribute(attributeName));

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionAttributes attributes = mock(ImmutableSessionAttributes.class);
        Object expected = new Object();

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttribute(attributeName)).thenReturn(expected);

        Object result = this.session.getAttributes().getAttribute(attributeName);

        Assert.assertSame(expected, result);

        verify(batch).close();
    }

    @Test
    public void setAttribute() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        String attributeName = "foo";
        Object attributeValue = "bar";

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.findSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, () -> this.session.getAttributes().setAttribute(attributeName, attributeValue));

        verify(batch).close();
        reset(batch);

        Session<Object> session = mock(Session.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Object expected = new Object();

        when(this.manager.findSession(this.id)).thenReturn(session);
        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.setAttribute(attributeName, attributeValue)).thenReturn(expected);

        Object result = this.session.getAttributes().setAttribute(attributeName, attributeValue);

        Assert.assertSame(expected, result);

        verify(session).close();
        verify(batch).close();
    }

    @Test
    public void removeAttribute() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        String attributeName = "foo";

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.findSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, () -> this.session.getAttributes().removeAttribute(attributeName));

        verify(batch).close();
        reset(batch);

        Session<Object> session = mock(Session.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Object expected = new Object();

        when(this.manager.findSession(this.id)).thenReturn(session);
        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.removeAttribute(attributeName)).thenReturn(expected);

        Object result = this.session.getAttributes().removeAttribute(attributeName);

        Assert.assertSame(expected, result);

        verify(session).close();
        verify(batch).close();
    }
}
