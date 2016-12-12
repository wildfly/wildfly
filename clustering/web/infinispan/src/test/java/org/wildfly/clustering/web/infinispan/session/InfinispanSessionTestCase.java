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
package org.wildfly.clustering.web.infinispan.session;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.session.Session;

/**
 * Unit test for {@link InfinispanSession}.
 *
 * @author paul
 */
public class InfinispanSessionTestCase {
    private final String id = "session";
    private final InvalidatableSessionMetaData metaData = mock(InvalidatableSessionMetaData.class);
    private final SessionAttributes attributes = mock(SessionAttributes.class);
    private final Remover<String> remover = mock(Remover.class);
    private final LocalContextFactory<Object> localContextFactory = mock(LocalContextFactory.class);
    private final AtomicReference<Object> localContextRef = new AtomicReference<>();

    private final Session<Object> session = new InfinispanSession<>(this.id, this.metaData, this.attributes, this.localContextRef, this.localContextFactory, this.remover);

    @Test
    public void getId() {
        assertSame(this.id, this.session.getId());
    }

    @Test
    public void getAttributes() {
        assertSame(this.attributes, this.session.getAttributes());
    }

    @Test
    public void getMetaData() {
        assertSame(this.metaData, this.session.getMetaData());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void invalidate() {
        when(this.metaData.invalidate()).thenReturn(true);

        this.session.invalidate();

        verify(this.remover).remove(this.id);
        reset(this.remover);

        when(this.metaData.invalidate()).thenReturn(false);

        this.session.invalidate();

        verify(this.remover, never()).remove(this.id);
    }

    @Test
    public void isValid() {
        when(this.metaData.isValid()).thenReturn(true);

        assertTrue(this.session.isValid());

        when(this.metaData.isValid()).thenReturn(false);

        assertFalse(this.session.isValid());
    }

    @Test
    public void close() {
        when(this.metaData.isValid()).thenReturn(true);

        this.session.close();

        verify(this.attributes).close();
        verify(this.metaData).setLastAccessedTime(any(Instant.class));

        reset(this.metaData, this.attributes);

        // Verify that session is not mutated if invalid
        when(this.metaData.isValid()).thenReturn(false);

        this.session.close();

        this.session.close();

        verify(this.attributes, never()).close();
        verify(this.metaData, never()).setLastAccessedTime(any(Instant.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getLocalContext() {
        Object expected = new Object();
        when(this.localContextFactory.createLocalContext()).thenReturn(expected);

        Object result = this.session.getLocalContext();

        assertSame(expected, result);

        reset(this.localContextFactory);

        result = this.session.getLocalContext();

        verifyZeroInteractions(this.localContextFactory);

        assertSame(expected, result);
    }
}
