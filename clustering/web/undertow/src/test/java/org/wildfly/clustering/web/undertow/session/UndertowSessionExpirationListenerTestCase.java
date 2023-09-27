/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.servlet.api.Deployment;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.SessionManager;

public class UndertowSessionExpirationListenerTestCase {

    @Test
    public void sessionExpired() {
        Deployment deployment = mock(Deployment.class);
        UndertowSessionManager manager = mock(UndertowSessionManager.class);
        SessionManager<Map<String, Object>, Batch> delegateManager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        SessionListener listener = mock(SessionListener.class);
        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionAttributes attributes = mock(ImmutableSessionAttributes.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        ArgumentCaptor<Session> capturedSession = ArgumentCaptor.forClass(Session.class);
        Recordable<ImmutableSessionMetaData> recorder = mock(Recordable.class);

        String expectedSessionId = "session";
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);

        Consumer<ImmutableSession> expirationListener = new UndertowSessionExpirationListener(deployment, listeners, recorder);

        when(deployment.getSessionManager()).thenReturn(manager);
        when(manager.getSessionManager()).thenReturn(delegateManager);
        when(delegateManager.getBatcher()).thenReturn(batcher);
        when(batcher.suspendBatch()).thenReturn(batch);
        when(session.getId()).thenReturn(expectedSessionId);
        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttributeNames()).thenReturn(Collections.emptySet());
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getCreationTime()).thenReturn(Instant.now());
        when(metaData.getLastAccessStartTime()).thenReturn(Instant.now());
        when(metaData.getTimeout()).thenReturn(Duration.ZERO);

        expirationListener.accept(session);

        verify(recorder).record(metaData);
        verify(batcher).suspendBatch();
        verify(listener).sessionDestroyed(capturedSession.capture(), isNull(), same(SessionListener.SessionDestroyedReason.TIMEOUT));
        verify(batcher).resumeBatch(batch);

        assertSame(expectedSessionId, capturedSession.getValue().getId());
        assertSame(manager, capturedSession.getValue().getSessionManager());
    }
}
