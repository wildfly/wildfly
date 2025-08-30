/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSessionActivationListener;

import io.undertow.UndertowOptions;
import io.undertow.connector.ByteBufferPool;
import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpServerConnection;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListener.SessionDestroyedReason;
import io.undertow.server.session.SessionListeners;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;
import io.undertow.util.Protocols;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.session.ImmutableSessionMetaData;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionMetaData;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;
import org.xnio.channels.Configurable;
import org.xnio.conduits.ConduitStreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;
import org.xnio.conduits.StreamSinkConduit;
import org.xnio.conduits.StreamSourceConduit;

/**
 * Unit test for {@link DistributableSession}.
 *
 * @author Paul Ferraro
 */
public class DistributableSessionTestCase {
    private final SessionMetaData metaData = mock(SessionMetaData.class);
    private final UndertowSessionManager manager = mock(UndertowSessionManager.class);
    private final SessionConfig config = mock(SessionConfig.class);
    private final Session<Map<String, Object>> session = mock(Session.class);
    private final SuspendedBatch suspendedBatch = mock(SuspendedBatch.class);
    private final Consumer<HttpServerExchange> closeTask = mock(Consumer.class);
    private final RecordableSessionManagerStatistics statistics = mock(RecordableSessionManagerStatistics.class);

    @Test
    public void getId() {
        String id = "id";

        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        doReturn(id).when(this.session).getId();

        String result = session.getId();

        verifyNoInteractions(this.suspendedBatch);

        assertSame(id, result);
    }

    @Test
    public void newSessionRequestDone() {
        Instant creationTime = Instant.now();
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(true).when(this.metaData).isNew();
        doReturn(creationTime).when(this.metaData).getCreationTime();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        HttpServerExchange exchange = new HttpServerExchange(null);
        Context<Batch> context = mock(Context.class);
        Batch batch = mock(Batch.class);
        ArgumentCaptor<Instant> capturedLastAccessStartTime = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> capturedLastAccessEndTime = ArgumentCaptor.forClass(Instant.class);

        doReturn(context).when(this.suspendedBatch).resumeWithContext();
        doReturn(batch).when(context).get();
        doReturn(true).when(this.session).isValid();
        doNothing().when(this.metaData).setLastAccess(capturedLastAccessStartTime.capture(), capturedLastAccessEndTime.capture());

        session.requestDone(exchange);

        Instant lastAccessStartTime = capturedLastAccessStartTime.getValue();
        Instant lastAccessEndTime = capturedLastAccessEndTime.getValue();

        Assert.assertSame(creationTime, lastAccessStartTime);
        Assert.assertNotSame(creationTime, lastAccessEndTime);
        Assert.assertFalse(lastAccessStartTime.isAfter(lastAccessEndTime));

        verify(this.session).close();
        verify(batch).close();
        verify(this.closeTask).accept(exchange);
    }

    @Test
    public void existingSessionRequestDone() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        HttpServerExchange exchange = new HttpServerExchange(null);
        Context<Batch> context = mock(Context.class);
        Batch batch = mock(Batch.class);
        ArgumentCaptor<Instant> capturedLastAccessStartTime = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> capturedLastAccessEndTime = ArgumentCaptor.forClass(Instant.class);

        doReturn(context).when(this.suspendedBatch).resumeWithContext();
        doReturn(batch).when(context).get();
        doReturn(true).when(this.session).isValid();
        doNothing().when(this.metaData).setLastAccess(capturedLastAccessStartTime.capture(), capturedLastAccessEndTime.capture());

        session.requestDone(exchange);

        Instant lastAccessStartTime = capturedLastAccessStartTime.getValue();
        Instant lastAccessEndTime = capturedLastAccessEndTime.getValue();

        Assert.assertNotSame(lastAccessStartTime, lastAccessEndTime);
        Assert.assertFalse(lastAccessStartTime.isAfter(lastAccessEndTime));

        verify(this.metaData).setLastAccess(any(Instant.class), any(Instant.class));
        verify(this.session).close();
        verify(batch).close();
        verify(this.closeTask).accept(exchange);
    }

    @Test
    public void invalidSessionRequestDone() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        verify(this.metaData).isNew();

        HttpServerExchange exchange = new HttpServerExchange(null);
        Context<Batch> context = mock(Context.class);
        Batch batch = mock(Batch.class);

        doReturn(context).when(this.suspendedBatch).resumeWithContext();
        doReturn(batch).when(context).get();
        doReturn(false).when(this.session).isValid();

        session.requestDone(exchange);

        verifyNoMoreInteractions(this.metaData);
        verify(this.session).close();
        verify(batch).close();
        verify(this.closeTask).accept(exchange);
    }

    @Test
    public void getCreationTime() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        verify(this.session).getMetaData();

        Instant now = Instant.now();

        doReturn(now).when(this.metaData).getCreationTime();

        long result = session.getCreationTime();

        assertEquals(now.toEpochMilli(), result);

        verify(this.session, times(2)).getMetaData();
        verifyNoMoreInteractions(this.session);
        verifyNoInteractions(this.suspendedBatch);
        verifyNoInteractions(this.closeTask);

        this.verifyInvalidMetaData(session, session::getCreationTime);
    }

    @Test
    public void getLastAccessedTime() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        verify(this.session).getMetaData();

        Instant now = Instant.now();

        doReturn(now).when(this.metaData).getLastAccessStartTime();

        long result = session.getLastAccessedTime();

        assertEquals(now.toEpochMilli(), result);

        verify(this.session, times(2)).getMetaData();
        verifyNoMoreInteractions(this.session);
        verifyNoInteractions(this.suspendedBatch);
        verifyNoInteractions(this.closeTask);

        this.verifyInvalidMetaData(session, session::getLastAccessedTime);
    }

    @Test
    public void getMaxInactiveInterval() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        verify(this.session).getMetaData();

        long expected = 3600L;

        doReturn(Duration.ofSeconds(expected)).when(this.metaData).getTimeout();

        long result = session.getMaxInactiveInterval();

        assertEquals(expected, result);

        verify(this.session, times(2)).getMetaData();
        verifyNoMoreInteractions(this.session);
        verifyNoInteractions(this.suspendedBatch);
        verifyNoInteractions(this.closeTask);

        this.verifyInvalidMetaData(session, session::getMaxInactiveInterval);
    }

    @Test
    public void setMaxInactiveInterval() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        verify(this.session).getMetaData();

        int interval = 3600;

        session.setMaxInactiveInterval(interval);

        verify(this.metaData).setTimeout(Duration.ofSeconds(interval));

        verifyNoInteractions(this.suspendedBatch);
        verify(this.session, never()).close();
        verify(this.closeTask, never()).accept(null);

        this.verifyInvalidMetaData(session, () -> session.setMaxInactiveInterval(interval));
    }

    @Test
    public void getAttributeNames() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        verify(this.session).getMetaData();

        Map<String, Object> attributes = mock(Map.class);
        Set<String> expected = Collections.singleton("name");

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(expected).when(attributes).keySet();

        Object result = session.getAttributeNames();

        assertSame(expected, result);

        verifyNoInteractions(this.suspendedBatch);
        verify(this.session, never()).close();
        verify(this.closeTask, never()).accept(null);

        this.verifyInvalidAttributes(session, session::getAttributeNames);
    }

    @Test
    public void getAttribute() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        verify(this.session).getMetaData();

        String name = "name";

        Map<String, Object> attributes = mock(Map.class);
        Object expected = new Object();

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(expected).when(attributes).get(name);

        Object result = session.getAttribute(name);

        assertSame(expected, result);

        verifyNoInteractions(this.suspendedBatch);
        verify(this.session, never()).close();
        verify(this.closeTask, never()).accept(null);

        this.verifyInvalidAttributes(session, () -> session.getAttribute(name));
    }

    @Test
    public void getAuthenticatedSessionAttribute() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        verify(this.session).getMetaData();

        String name = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";

        Map<String, Object> attributes = mock(Map.class);
        Account account = mock(Account.class);
        AuthenticatedSession auth = new AuthenticatedSession(account, HttpServletRequest.FORM_AUTH);

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(auth).when(attributes).get(name);

        AuthenticatedSession result = (AuthenticatedSession) session.getAttribute(name);

        assertSame(account, result.getAccount());
        assertSame(HttpServletRequest.FORM_AUTH, result.getMechanism());

        verifyNoInteractions(this.suspendedBatch);
        verify(this.session, never()).close();
        verify(this.closeTask, never()).accept(null);

        AuthenticatedSession expected = new AuthenticatedSession(account, HttpServletRequest.BASIC_AUTH);
        Map<String, Object> localContext = Collections.singletonMap(name, expected);

        doReturn(null).when(attributes).get(name);
        doReturn(localContext).when(this.session).getContext();

        result = (AuthenticatedSession) session.getAttribute(name);

        assertSame(expected, result);

        verifyNoInteractions(this.suspendedBatch);
        verify(this.session, never()).close();
        verify(this.closeTask, never()).accept(null);

        this.verifyInvalidAttributes(session, () -> session.getAttribute(name));
    }

    @Test
    public void getWebSocketChannelsSessionAttribute() {
        this.getLocalContextSessionAttribute(AbstractSession.WEB_SOCKET_CHANNELS_ATTRIBUTE);
    }

    private void getLocalContextSessionAttribute(String name) {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        Map<String, Object> attributes = mock(Map.class);
        Object expected = new Object();
        Map<String, Object> localContext = Collections.singletonMap(name, expected);

        doReturn(localContext).when(this.session).getContext();

        Object result = session.getAttribute(name);

        assertSame(expected, result);

        verifyNoInteractions(this.suspendedBatch);
        verify(attributes, never()).get(name);
    }

    @Test
    public void setAttribute() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        verify(this.session).getMetaData();

        String name = "name";
        Integer value = 1;

        Map<String, Object> attributes = mock(Map.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);

        Object expected = new Object();

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(expected).when(attributes).put(name, value);
        doReturn(listeners).when(this.manager).getSessionListeners();

        Object result = session.setAttribute(name, value);

        assertSame(expected, result);

        verify(listener, never()).attributeAdded(session, name, value);
        verify(listener).attributeUpdated(session, name, value, expected);
        verify(listener, never()).attributeRemoved(same(session), same(name), any());
        verifyNoInteractions(this.suspendedBatch);
        verify(this.session, never()).close();
        verify(this.closeTask, never()).accept(null);

        this.verifyInvalidAttributes(session, () -> session.setAttribute(name, value));
    }

    @Test
    public void setNewAttribute() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        String name = "name";
        Integer value = 1;

        Map<String, Object> attributes = mock(Map.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        Object expected = null;

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(expected).when(attributes).put(name, value);
        doReturn(listeners).when(this.manager).getSessionListeners();

        Object result = session.setAttribute(name, value);

        assertSame(expected, result);

        verify(listener).attributeAdded(session, name, value);
        verify(listener, never()).attributeUpdated(same(session), same(name), same(value), any());
        verify(listener, never()).attributeRemoved(same(session), same(name), any());
        verifyNoInteractions(this.suspendedBatch);
    }

    @Test
    public void setNullAttribute() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        String name = "name";
        Object value = null;

        Map<String, Object> attributes = mock(Map.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        Object expected = new Object();

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(expected).when(attributes).remove(name);
        doReturn(listeners).when(this.manager).getSessionListeners();

        Object result = session.setAttribute(name, value);

        assertSame(expected, result);

        verify(listener, never()).attributeAdded(session, name, value);
        verify(listener, never()).attributeUpdated(same(session), same(name), same(value), any());
        verify(listener).attributeRemoved(session, name, expected);
        verifyNoInteractions(this.suspendedBatch);
    }

    @Test
    public void setSameAttribute() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        String name = "name";
        Integer value = 1;

        Map<String, Object> attributes = mock(Map.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        Object expected = value;

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(expected).when(attributes).put(name, value);
        doReturn(listeners).when(this.manager).getSessionListeners();

        Object result = session.setAttribute(name, value);

        assertSame(expected, result);

        verify(listener, never()).attributeAdded(session, name, value);
        verify(listener, never()).attributeUpdated(same(session), same(name), same(value), any());
        verify(listener, never()).attributeRemoved(same(session), same(name), any());
        verifyNoInteractions(this.suspendedBatch);
    }

    @Test
    public void setAuthenticatedSessionAttribute() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        String name = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";
        Account account = mock(Account.class);
        AuthenticatedSession auth = new AuthenticatedSession(account, HttpServletRequest.FORM_AUTH);

        Map<String, Object> attributes = mock(Map.class);
        Account oldAccount = mock(Account.class);
        AuthenticatedSession oldAuth = new AuthenticatedSession(oldAccount, HttpServletRequest.FORM_AUTH);
        ArgumentCaptor<AuthenticatedSession> capturedAuth = ArgumentCaptor.forClass(AuthenticatedSession.class);

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(oldAuth).when(attributes).put(same(name), capturedAuth.capture());

        AuthenticatedSession result = (AuthenticatedSession) session.setAttribute(name, auth);

        assertSame(auth.getAccount(), capturedAuth.getValue().getAccount());
        assertSame(auth.getMechanism(), capturedAuth.getValue().getMechanism());

        assertSame(oldAccount, result.getAccount());
        assertSame(HttpServletRequest.FORM_AUTH, result.getMechanism());

        verifyNoInteractions(this.suspendedBatch);
        verify(this.session, never()).close();
        verify(this.closeTask, never()).accept(null);

        reset(attributes);

        capturedAuth = ArgumentCaptor.forClass(AuthenticatedSession.class);

        doReturn(null).when(attributes).put(same(name), capturedAuth.capture());

        result = (AuthenticatedSession) session.setAttribute(name, auth);

        assertSame(auth.getAccount(), capturedAuth.getValue().getAccount());
        assertSame(auth.getMechanism(), capturedAuth.getValue().getMechanism());

        assertNull(result);

        verifyNoInteractions(this.suspendedBatch);
        verify(this.session, never()).close();
        verify(this.closeTask, never()).accept(null);

        reset(attributes);

        auth = new AuthenticatedSession(account, HttpServletRequest.BASIC_AUTH);
        AuthenticatedSession oldSession = new AuthenticatedSession(oldAccount, HttpServletRequest.BASIC_AUTH);

        Map<String, Object> localContext = new HashMap<>();
        localContext.put(name, oldSession);

        doReturn(localContext).when(this.session).getContext();

        result = (AuthenticatedSession) session.setAttribute(name, auth);

        assertSame(auth, localContext.get(name));

        verifyNoInteractions(this.suspendedBatch);
        verify(this.session, never()).close();
        verify(this.closeTask, never()).accept(null);

        this.verifyInvalidAttributes(session, () -> session.setAttribute(name, oldAuth));
    }

    @Test
    public void setWebSocketChannelsSessionAttribute() {
        this.setLocalContextSessionAttribute(AbstractSession.WEB_SOCKET_CHANNELS_ATTRIBUTE);
    }

    private void setLocalContextSessionAttribute(String name) {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        Object newValue = new Object();
        Object oldValue = new Object();

        Map<String, Object> attributes = mock(Map.class);
        Map<String, Object> localContext = new HashMap<>();
        localContext.put(name, oldValue);

        doReturn(localContext).when(this.session).getContext();

        Object result = session.setAttribute(name, newValue);

        assertSame(oldValue, result);

        assertSame(newValue, localContext.get(name));
        verify(attributes, never()).put(name, newValue);
        verifyNoInteractions(this.suspendedBatch);
    }

    @Test
    public void removeAttribute() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        String name = "name";

        Map<String, Object> attributes = mock(Map.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        Object expected = new Object();

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(expected).when(attributes).remove(name);
        doReturn(listeners).when(this.manager).getSessionListeners();

        Object result = session.removeAttribute(name);

        assertSame(expected, result);

        verify(listener).attributeRemoved(session, name, expected);
        verifyNoInteractions(this.suspendedBatch);
        verify(this.session, never()).close();
        verify(this.closeTask, never()).accept(null);

        this.verifyInvalidAttributes(session, () -> session.removeAttribute(name));
    }

    @Test
    public void removeNonExistingAttribute() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        String name = "name";

        Map<String, Object> attributes = mock(Map.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(null).when(attributes).remove(name);
        doReturn(listeners).when(this.manager).getSessionListeners();

        Object result = session.removeAttribute(name);

        assertNull(result);

        verify(listener, never()).attributeRemoved(same(session), same(name), any());
        verifyNoInteractions(this.suspendedBatch);
    }

    @Test
    public void removeAuthenticatedSessionAttribute() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        String name = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";

        Map<String, Object> attributes = mock(Map.class);
        Account oldAccount = mock(Account.class);
        AuthenticatedSession oldAuth = new AuthenticatedSession(oldAccount, HttpServletRequest.FORM_AUTH);

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(oldAuth).when(attributes).remove(same(name));

        AuthenticatedSession result = (AuthenticatedSession) session.removeAttribute(name);

        assertSame(oldAccount, result.getAccount());
        assertSame(HttpServletRequest.FORM_AUTH, result.getMechanism());
        verifyNoInteractions(this.suspendedBatch);

        reset(attributes);

        Map<String, Object> localContext = new HashMap<>();
        AuthenticatedSession oldSession = new AuthenticatedSession(oldAccount, HttpServletRequest.BASIC_AUTH);
        localContext.put(name, oldSession);

        doReturn(null).when(attributes).remove(same(name));
        doReturn(localContext).when(this.session).getContext();

        result = (AuthenticatedSession) session.removeAttribute(name);

        assertSame(result, oldSession);
        assertNull(localContext.get(name));
        verifyNoInteractions(this.suspendedBatch);

        reset(attributes);

        result = (AuthenticatedSession) session.removeAttribute(name);

        assertNull(result);

        verifyNoInteractions(this.suspendedBatch);
        verify(this.session, never()).close();
        verify(this.closeTask, never()).accept(null);

        this.verifyInvalidAttributes(session, () -> session.removeAttribute(name));
    }

    @Test
    public void removeWebSocketChannelsSessionAttribute() {
        this.removeLocalContextSessionAttribute(AbstractSession.WEB_SOCKET_CHANNELS_ATTRIBUTE);
    }

    private void removeLocalContextSessionAttribute(String name) {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        Object oldValue = new Object();

        Map<String, Object> attributes = mock(Map.class);
        Map<String, Object> localContext = new HashMap<>();
        localContext.put(name, oldValue);

        doReturn(localContext).when(this.session).getContext();

        Object result = session.removeAttribute(name);

        assertSame(oldValue, result);

        assertNull(localContext.get(name));
        verify(attributes, never()).remove(name);
        verifyNoInteractions(this.suspendedBatch);
    }

    @Test
    public void invalidate() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        HttpServerExchange exchange = new HttpServerExchange(null);

        Context<Batch> context = mock(Context.class);
        Batch batch = mock(Batch.class);
        SessionListener listener = mock(SessionListener.class);
        Map<String, Object> attributes = mock(Map.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String sessionId = "session";
        String attributeName = "attribute";
        Object attributeValue = mock(HttpSessionActivationListener.class);
        Recordable<ImmutableSessionMetaData> recorder = mock(Recordable.class);

        doReturn(listeners).when(this.manager).getSessionListeners();
        doReturn(true).when(this.session).isValid();
        doReturn(sessionId).when(this.session).getId();
        doReturn(attributes).when(this.session).getAttributes();
        doReturn(Set.of(Map.entry("attribute", attributeValue))).when(attributes).entrySet();
        doReturn(recorder).when(this.statistics).getInactiveSessionRecorder();
        doReturn(context).when(this.suspendedBatch).resumeWithContext();
        doReturn(batch).when(context).get();

        session.invalidate(exchange);

        verify(recorder).record(this.metaData);
        verify(this.session).invalidate();
        verify(this.config).clearSession(exchange, sessionId);
        verify(listener).sessionDestroyed(session, exchange, SessionDestroyedReason.INVALIDATED);
        verify(listener).attributeRemoved(session, attributeName, attributeValue);
        verify(batch).close();
        verify(this.closeTask).accept(exchange);
    }

    @Test
    public void invalidateInvalid() {
        String sessionId = "session";
        HttpServerExchange exchange = new HttpServerExchange(null);
        Context<Batch> context = mock(Context.class);
        Batch batch = mock(Batch.class);

        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();
        doReturn(sessionId).when(this.session).getId();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        doReturn(context).when(this.suspendedBatch).resumeWithContext();
        doReturn(batch).when(context).get();
        doReturn(false).when(this.session).isValid();
        doThrow(IllegalStateException.class).when(this.session).invalidate();

        assertThrows(IllegalStateException.class, () -> session.invalidate(exchange));

        verify(this.config).clearSession(exchange, sessionId);
        verify(batch).close();
        verify(this.closeTask).accept(exchange);
    }

    @Test
    public void getSessionManager() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        assertSame(this.manager, session.getSessionManager());
    }

    @Test
    public void changeSessionId() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
        Context<Batch> context = mock(Context.class);
        SessionManager<Map<String, Object>> manager = mock(SessionManager.class);
        Supplier<String> identifierFactory = mock(Supplier.class);
        Session<Map<String, Object>> newSession = mock(Session.class);
        Map<String, Object> oldAttributes = mock(Map.class);
        Map<String, Object> newAttributes = mock(Map.class);
        SessionMetaData oldMetaData = mock(SessionMetaData.class);
        SessionMetaData newMetaData = mock(SessionMetaData.class);
        Map<String, Object> oldContext = new HashMap<>();
        oldContext.put("foo", "bar");
        Map<String, Object> newContext = new HashMap<>();
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String oldSessionId = "old";
        String newSessionId = "new";
        Instant now = Instant.now();
        Duration interval = Duration.ofSeconds(10L);

        doReturn(context).when(this.suspendedBatch).resumeWithContext();
        doReturn(manager).when(this.manager).getSessionManager();
        doReturn(identifierFactory).when(manager).getIdentifierFactory();
        doReturn(newSessionId).when(identifierFactory).get();
        doReturn(newSession).when(manager).createSession(newSessionId);
        doReturn(oldAttributes).when(this.session).getAttributes();
        doReturn(oldMetaData).when(this.session).getMetaData();
        doReturn(newAttributes).when(newSession).getAttributes();
        doReturn(newMetaData).when(newSession).getMetaData();
        doReturn(now).when(oldMetaData).getLastAccessStartTime();
        doReturn(now).when(oldMetaData).getLastAccessEndTime();
        doReturn(interval).when(oldMetaData).getTimeout();
        doReturn(oldSessionId).when(this.session).getId();
        doReturn(newSessionId).when(newSession).getId();
        doReturn(oldContext).when(this.session).getContext();
        doReturn(newContext).when(newSession).getContext();
        doReturn(listeners).when(this.manager).getSessionListeners();

        String result = session.changeSessionId(exchange, config);

        assertSame(newSessionId, result);

        verify(newMetaData).setLastAccess(now, now);
        verify(newMetaData).setTimeout(interval);
        verify(newAttributes).putAll(oldAttributes);
        verify(config).setSessionId(exchange, newSessionId);
        assertEquals(oldContext, newContext);
        verify(this.session).invalidate();
        verify(newSession, never()).invalidate();
        verify(listener).sessionIdChanged(session, oldSessionId);
        verify(this.suspendedBatch).resumeWithContext();
        verify(context).close();
        verifyNoMoreInteractions(this.suspendedBatch);
    }

    public void changeSessionIdResponseCommitted() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();
        doReturn(true).when(this.session).isValid();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        // Ugh - all this, just to get HttpServerExchange.isResponseStarted() to return true
        Configurable configurable = mock(Configurable.class);
        StreamSourceConduit sourceConduit = mock(StreamSourceConduit.class);
        ConduitStreamSourceChannel sourceChannel = new ConduitStreamSourceChannel(configurable, sourceConduit);
        StreamSinkConduit sinkConduit = mock(StreamSinkConduit.class);
        ConduitStreamSinkChannel sinkChannel = new ConduitStreamSinkChannel(configurable, sinkConduit);
        StreamConnection stream = mock(StreamConnection.class);

        doReturn(sourceChannel).when(stream).getSourceChannel();
        doReturn(sinkChannel).when(stream).getSinkChannel();

        ByteBufferPool bufferPool = mock(ByteBufferPool.class);
        HttpHandler handler = mock(HttpHandler.class);
        HttpServerConnection connection = new HttpServerConnection(stream, bufferPool, handler, OptionMap.create(UndertowOptions.ALWAYS_SET_DATE, false), 0, null);
        HttpServerExchange exchange = new HttpServerExchange(connection);
        exchange.setProtocol(Protocols.HTTP_1_1);
        exchange.getResponseChannel();

        SessionConfig config = mock(SessionConfig.class);

        Assert.assertThrows(IllegalStateException.class, () -> session.changeSessionId(exchange, config));
    }

    @Test
    public void changeSessionIdConcurrentInvalidate() {
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(false).when(this.metaData).isNew();

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.suspendedBatch, this.closeTask, this.statistics);

        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
        Context<Batch> context = mock(Context.class);
        SessionManager<Map<String, Object>> manager = mock(SessionManager.class);
        Supplier<String> identifierFactory = mock(Supplier.class);
        Session<Map<String, Object>> newSession = mock(Session.class);
        Map<String, Object> oldAttributes = mock(Map.class);
        Map<String, Object> newAttributes = mock(Map.class);
        SessionMetaData oldMetaData = mock(SessionMetaData.class);
        SessionMetaData newMetaData = mock(SessionMetaData.class);
        Map<String, Object> oldContext = new HashMap<>();
        oldContext.put("foo", "bar");
        Map<String, Object> newContext = new HashMap<>();
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String oldSessionId = "old";
        String newSessionId = "new";
        Instant now = Instant.now();
        Duration interval = Duration.ofSeconds(10L);

        doReturn(context).when(this.suspendedBatch).resumeWithContext();
        doReturn(manager).when(this.manager).getSessionManager();
        doReturn(identifierFactory).when(manager).getIdentifierFactory();
        doReturn(newSessionId).when(identifierFactory).get();
        doReturn(newSession).when(manager).createSession(newSessionId);
        doReturn(oldAttributes).when(this.session).getAttributes();
        doReturn(oldMetaData).when(this.session).getMetaData();
        doReturn(newAttributes).when(newSession).getAttributes();
        doReturn(newMetaData).when(newSession).getMetaData();
        doReturn(now).when(oldMetaData).getLastAccessStartTime();
        doReturn(now).when(oldMetaData).getLastAccessEndTime();
        doReturn(interval).when(oldMetaData).getTimeout();
        doReturn(oldSessionId).when(this.session).getId();
        doReturn(newSessionId).when(newSession).getId();
        doReturn(oldContext).when(this.session).getContext();
        doReturn(newContext).when(newSession).getContext();

        doThrow(IllegalStateException.class).when(this.session).invalidate();

        assertThrows(IllegalStateException.class, () -> session.changeSessionId(exchange, config));

        verify(listener, never()).sessionIdChanged(session, oldSessionId);
        verify(this.session).close();
        verify(this.closeTask).accept(exchange);
        verify(newSession).invalidate();
        verify(this.suspendedBatch, times(2)).resumeWithContext();
        verify(context, times(2)).close();
        verifyNoMoreInteractions(this.suspendedBatch);
    }

    void verifyInvalidMetaData(io.undertow.server.session.Session session, ThrowingRunnable action) {
        this.verifyInvalidSession(session, action, Session::getMetaData);
    }

    void verifyInvalidAttributes(io.undertow.server.session.Session session, ThrowingRunnable action) {
        this.verifyInvalidSession(session, action, Session::getAttributes);
    }

    void verifyInvalidSession(io.undertow.server.session.Session session, ThrowingRunnable action, Consumer<Session<Map<String, Object>>> verifiedAction) {
        reset(this.session);

        verifiedAction.accept(doThrow(IllegalStateException.class).when(this.session));
        doReturn(true, false).when(this.session).isValid();

        assertThrows(IllegalStateException.class, action);

        // Session should not close, since it is still valid
        verifiedAction.accept(verify(this.session));
        verify(this.session).isValid();
        verifyNoMoreInteractions(this.session);
        verifyNoInteractions(this.suspendedBatch);
        verifyNoInteractions(this.closeTask);

        Context<Batch> context = mock(Context.class);
        Batch batch = mock(Batch.class);

        doReturn(context).when(this.suspendedBatch).resumeWithContext();
        doReturn(batch).when(context).get();

        assertThrows(IllegalStateException.class, action);

        verifiedAction.accept(verify(this.session, times(2)));
        verify(this.session, times(2)).isValid();
        verify(this.session).close();
        verify(batch).close();
        verify(this.closeTask).accept(null);

        assertThrows(IllegalStateException.class, action);

        verifiedAction.accept(verify(this.session, times(3)));
        verify(this.session, times(3)).isValid();
        verifyNoMoreInteractions(this.session);
        verifyNoMoreInteractions(batch);
        verifyNoMoreInteractions(this.closeTask);
    }
}
