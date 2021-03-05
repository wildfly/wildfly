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

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionManager;

/**
 * Unit test for {@link OOBSessionAttributes}.
 * @author Paul Ferraro
 */
public class OOBSessionAttributesTestCase {
    private final SessionManager<Object, Batch> manager = mock(SessionManager.class);
    private final String id = "ABC123";

    private final SessionAttributes attributes = new OOBSessionAttributes<>(this.manager, this.id);

    @Test
    public void getAttributeNames() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(this.id)).thenReturn(null);

        Assert.assertThrows(IllegalStateException.class, this.attributes::getAttributeNames);

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionAttributes attributes = mock(ImmutableSessionAttributes.class);
        Set<String> expected = Collections.singleton("foo");

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttributeNames()).thenReturn(expected);

        Set<String> result = this.attributes.getAttributeNames();

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

        Assert.assertThrows(IllegalStateException.class, () -> this.attributes.getAttribute(attributeName));

        verify(batch).close();
        reset(batch);

        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionAttributes attributes = mock(ImmutableSessionAttributes.class);
        Object expected = new Object();

        when(this.manager.readSession(this.id)).thenReturn(session);
        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttribute(attributeName)).thenReturn(expected);

        Object result = this.attributes.getAttribute(attributeName);

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

        Assert.assertThrows(IllegalStateException.class, () -> this.attributes.setAttribute(attributeName, attributeValue));

        verify(batch).close();
        reset(batch);

        Session<Object> session = mock(Session.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Object expected = new Object();

        when(this.manager.findSession(this.id)).thenReturn(session);
        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.setAttribute(attributeName, attributeValue)).thenReturn(expected);

        Object result = this.attributes.setAttribute(attributeName, attributeValue);

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

        Assert.assertThrows(IllegalStateException.class, () -> this.attributes.removeAttribute(attributeName));

        verify(batch).close();
        reset(batch);

        Session<Object> session = mock(Session.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Object expected = new Object();

        when(this.manager.findSession(this.id)).thenReturn(session);
        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.removeAttribute(attributeName)).thenReturn(expected);

        Object result = this.attributes.removeAttribute(attributeName);

        Assert.assertSame(expected, result);

        verify(session).close();
        verify(batch).close();
    }
}
