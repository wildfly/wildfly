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

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
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
}
