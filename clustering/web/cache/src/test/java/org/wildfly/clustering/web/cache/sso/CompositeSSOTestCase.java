/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.sso;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.Test;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;

public class CompositeSSOTestCase {
    private final String id = "id";
    private final String authentication = "auth";
    private final Sessions<String, String> sessions = mock(Sessions.class);
    private final AtomicReference<Object> localContext = new AtomicReference<>();
    private final Supplier<Object> localContextFactory = mock(Supplier.class);
    private final Remover<String> remover = mock(Remover.class);

    private final SSO<String, String, String, Object> sso = new CompositeSSO<>(this.id, this.authentication, this.sessions, this.localContext, this.localContextFactory, this.remover);

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
        when(this.localContextFactory.get()).thenReturn(expected);

        Object result = this.sso.getLocalContext();

        assertSame(expected, result);

        reset(this.localContextFactory);

        result = this.sso.getLocalContext();

        verifyNoInteractions(this.localContextFactory);

        assertSame(expected, result);
    }
}
