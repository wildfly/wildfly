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

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.servlet.api.Deployment;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.clustering.web.session.SessionManager;

public class UndertowSessionExpirationListenerTestCase {

    @Test
    public void sessionExpired() {
        Deployment deployment = mock(Deployment.class);
        UndertowSessionManager manager = mock(UndertowSessionManager.class);
        SessionManager<LocalSessionContext, Batch> delegateManager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
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
        when(manager.getSessionManager()).thenReturn(delegateManager);
        when(delegateManager.getBatcher()).thenReturn(batcher);
        when(batcher.suspendBatch()).thenReturn(batch);
        when(session.getId()).thenReturn(expectedSessionId);
        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttributeNames()).thenReturn(Collections.emptySet());
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getCreationTime()).thenReturn(Instant.now());
        when(metaData.getLastAccessedTime()).thenReturn(Instant.now());
        when(metaData.getMaxInactiveInterval()).thenReturn(Duration.ZERO);

        expirationListener.sessionExpired(session);

        verify(batcher).suspendBatch();
        verify(listener).sessionDestroyed(capturedSession.capture(), isNull(), same(SessionListener.SessionDestroyedReason.TIMEOUT));
        verify(batcher).resumeBatch(batch);

        assertSame(expectedSessionId, capturedSession.getValue().getId());
        assertSame(manager, capturedSession.getValue().getSessionManager());
    }
}
