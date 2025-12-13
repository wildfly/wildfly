/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.spec.ServletContextImpl;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.ImmutableSessionMetaData;
import org.wildfly.clustering.session.SessionManager;

public class UndertowSessionExpirationListenerTestCase {

    @Test
    public void sessionExpired() {
        Deployment deployment = mock(Deployment.class);
        UndertowSessionManager manager = mock(UndertowSessionManager.class);
        SessionManager<Map<String, Object>> delegateManager = mock(SessionManager.class);
        SessionListener listener = mock(SessionListener.class);
        ServletContextImpl context = mock(ServletContextImpl.class);
        ImmutableSession session = mock(ImmutableSession.class);
        Map<String, Object> attributes = mock(Map.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        ArgumentCaptor<Session> capturedSession = ArgumentCaptor.forClass(Session.class);
        Recordable<ImmutableSessionMetaData> recorder = mock(Recordable.class);

        String expectedSessionId = "session";
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);

        Consumer<ImmutableSession> expirationListener = new UndertowSessionExpirationListener(deployment, listeners, recorder);
        doReturn(context).when(deployment).getServletContext();
        doReturn(Thread.currentThread().getContextClassLoader()).when(context).getClassLoader();
        doReturn(manager).when(deployment).getSessionManager();
        doReturn(delegateManager).when(manager).getSessionManager();
        doReturn(expectedSessionId).when(session).getId();
        doReturn(attributes).when(session).getAttributes();
        doReturn(Set.of()).when(attributes).entrySet();
        doReturn(metaData).when(session).getMetaData();
        doReturn(Instant.now()).when(metaData).getCreationTime();
        doReturn(Optional.of(Instant.now())).when(metaData).getLastAccessStartTime();
        doReturn(Optional.empty()).when(metaData).getMaxIdle();

        expirationListener.accept(session);

        verify(recorder).record(metaData);
        verify(listener).sessionDestroyed(capturedSession.capture(), isNull(), same(SessionListener.SessionDestroyedReason.TIMEOUT));

        assertSame(expectedSessionId, capturedSession.getValue().getId());
        assertSame(manager, capturedSession.getValue().getSessionManager());
    }
}
