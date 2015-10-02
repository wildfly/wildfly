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
package org.wildfly.clustering.web.undertow.session;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.SessionExpirationListener;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.Deployment;

public class UndertowSessionExpirationListenerTestCase {

    @Test
    public void sessionExpired() {
        Deployment deployment = mock(Deployment.class);
        SessionManager manager = mock(SessionManager.class);
        SessionListener listener = mock(SessionListener.class);
        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionAttributes attributes = mock(ImmutableSessionAttributes.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        ArgumentCaptor<Session> capturedSession = ArgumentCaptor.forClass(Session.class);

        String expectedSessionId = "session";
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);

        SessionExpirationListener expirationListener = new UndertowSessionExpirationListener(deployment, listeners);

        when(deployment.getSessionManager()).thenReturn(manager);
        when(session.getId()).thenReturn(expectedSessionId);
        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttributeNames()).thenReturn(Collections.emptySet());
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getCreationTime()).thenReturn(Instant.now());
        when(metaData.getLastAccessedTime()).thenReturn(Instant.now());
        when(metaData.getMaxInactiveInterval()).thenReturn(Duration.ZERO);

        expirationListener.sessionExpired(session);

        verify(listener).sessionDestroyed(capturedSession.capture(), isNull(HttpServerExchange.class), same(SessionListener.SessionDestroyedReason.TIMEOUT));

        assertSame(expectedSessionId, capturedSession.getValue().getId());
        assertSame(manager, capturedSession.getValue().getSessionManager());
    }
}
