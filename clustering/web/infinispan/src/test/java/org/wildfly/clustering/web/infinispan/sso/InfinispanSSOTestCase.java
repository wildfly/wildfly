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
package org.wildfly.clustering.web.infinispan.sso;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;

public class InfinispanSSOTestCase {
    private final String id = "id";
    private final String authentication = "auth";
    private final Sessions<String, String> sessions = mock(Sessions.class);
    private final AtomicReference<Object> localContext = new AtomicReference<>();
    private final LocalContextFactory<Object> localContextFactory = mock(LocalContextFactory.class);
    private final Remover<String> remover = mock(Remover.class);

    private final SSO<String, String, String, Object> sso = new InfinispanSSO<>(this.id, this.authentication, this.sessions, this.localContext, this.localContextFactory, this.remover);

    @Test
    public void getId() {
        assertSame(this.id, this.sso.getId());
    }

    @Test
    public void getAuthentication() {
        assertSame(this.authentication, this.sso.getAuthentication());
    }

    @Test
    public void getSessions() {
        assertSame(this.sessions, this.sso.getSessions());
    }

    @Test
    public void invalidate() {
        this.sso.invalidate();

        verify(this.remover).remove(this.id);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getLocalContext() {
        Object expected = new Object();
        when(this.localContextFactory.createLocalContext()).thenReturn(expected);

        Object result = this.sso.getLocalContext();

        assertSame(expected, result);

        reset(this.localContextFactory);

        result = this.sso.getLocalContext();

        verifyZeroInteractions(this.localContextFactory);

        assertSame(expected, result);
    }
}
