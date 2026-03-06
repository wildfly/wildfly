/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

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
import org.mockito.InOrder;
import org.wildfly.clustering.function.Consumer;
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
    private final Session<Map<String, Object>> session = mock(Session.class);
    private final Consumer<HttpServerExchange> closeTask = mock(Consumer.class);
    private final SessionListener listener = mock(SessionListener.class);
    private final Instant creationTime = Instant.now();

    private UndertowSession getSession(Optional<Instant> lastAccessStart) {
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(this.listener);

        doReturn(listeners).when(this.manager).getSessionListeners();
        doReturn(this.metaData).when(this.session).getMetaData();
        doReturn(this.creationTime).when(this.metaData).getCreationTime();
        doReturn(lastAccessStart).when(this.metaData).getLastAccessStartTime();

        UndertowSession session = new DistributableSession(this.manager, this.session, this.closeTask);

        verify(this.session).getMetaData();
        verify(this.metaData).getLastAccessStartTime();
        if (lastAccessStart.isEmpty()) {
            verify(this.metaData).getCreationTime();
        }
        verifyNoMoreInteractions(this.session, this.metaData, this.session, this.closeTask);

        doReturn(true).when(this.session).isValid();

        return session;
    }

    @Test
    public void getId() {
        String id = "id";
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        doReturn(id).when(this.session).getId();

        String result = session.getId();

        assertSame(id, result);
    }

    @Test
    public void newSessionRequestDone() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        HttpServerExchange exchange = new HttpServerExchange(null);
        ArgumentCaptor<Instant> capturedLastAccessStartTime = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> capturedLastAccessEndTime = ArgumentCaptor.forClass(Instant.class);

        doReturn(true).when(this.session).isValid();
        doNothing().when(this.metaData).setLastAccess(capturedLastAccessStartTime.capture(), capturedLastAccessEndTime.capture());

        session.requestDone(exchange);

        Instant lastAccessStartTime = capturedLastAccessStartTime.getValue();
        Instant lastAccessEndTime = capturedLastAccessEndTime.getValue();

        Assert.assertSame(this.creationTime, lastAccessStartTime);
        Assert.assertNotSame(this.creationTime, lastAccessEndTime);
        Assert.assertFalse(lastAccessStartTime.isAfter(lastAccessEndTime));

        verify(this.session).close();
        verify(this.closeTask, only()).accept(exchange);

        // Verify no double close
        session.requestDone(exchange);

        verify(this.session).close();
        verifyNoMoreInteractions(this.closeTask);
    }

    @Test
    public void existingSessionRequestDone() {
        Instant lastAccessTime = Instant.now();
        io.undertow.server.session.Session session = this.getSession(Optional.of(lastAccessTime));

        HttpServerExchange exchange = new HttpServerExchange(null);
        ArgumentCaptor<Instant> capturedLastAccessStartTime = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> capturedLastAccessEndTime = ArgumentCaptor.forClass(Instant.class);

        doReturn(true).when(this.session).isValid();
        doNothing().when(this.metaData).setLastAccess(capturedLastAccessStartTime.capture(), capturedLastAccessEndTime.capture());

        session.requestDone(exchange);

        Instant lastAccessStartTime = capturedLastAccessStartTime.getValue();
        Instant lastAccessEndTime = capturedLastAccessEndTime.getValue();

        Assert.assertNotSame(lastAccessStartTime, lastAccessEndTime);
        Assert.assertFalse(lastAccessStartTime.isAfter(lastAccessEndTime));

        verify(this.metaData).setLastAccess(any(Instant.class), any(Instant.class));
        verify(this.session).close();
        verify(this.closeTask, only()).accept(exchange);

        // Verify no double close
        session.requestDone(exchange);

        verify(this.session).close();
        verifyNoMoreInteractions(this.closeTask);
    }

    @Test
    public void invalidSessionRequestDone() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        HttpServerExchange exchange = new HttpServerExchange(null);

        doReturn(false).when(this.session).isValid();

        session.requestDone(exchange);

        verifyNoMoreInteractions(this.metaData);
        verify(this.session).close();
        verify(this.closeTask, only()).accept(exchange);

        // Verify no double close
        session.requestDone(exchange);

        verify(this.session).close();
        verifyNoMoreInteractions(this.closeTask);
    }

    @Test
    public void getCreationTime() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        long result = session.getCreationTime();

        assertEquals(this.creationTime.toEpochMilli(), result);

        verifyNoInteractions(this.closeTask);

        this.verifyWhenInvalid(session::getCreationTime);
    }

    @Test
    public void getLastAccessedTime() {
        Instant expected = Instant.now();

        io.undertow.server.session.Session session = this.getSession(Optional.of(expected));

        long result = session.getLastAccessedTime();

        assertEquals(expected.toEpochMilli(), result);

        verifyNoInteractions(this.closeTask);

        this.verifyWhenInvalid(session::getLastAccessedTime);
    }

    @Test
    public void getMaxInactiveInterval() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        long expected = 3600L;

        doReturn(Optional.of(Duration.ofSeconds(expected))).when(this.metaData).getMaxIdle();

        long result = session.getMaxInactiveInterval();

        assertEquals(expected, result);

        verifyNoInteractions(this.closeTask);

        this.verifyWhenInvalid(session::getLastAccessedTime);
    }

    @Test
    public void setMaxInactiveInterval() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        int interval = 3600;

        session.setMaxInactiveInterval(interval);

        verify(this.metaData).setMaxIdle(Duration.ofSeconds(interval));

        session.setMaxInactiveInterval(0);

        verify(this.metaData).setMaxIdle(Duration.ZERO);

        session.setMaxInactiveInterval(-1);

        verify(this.metaData, times(2)).setMaxIdle(Duration.ZERO);

        verifyNoInteractions(this.closeTask);

        this.verifyWhenInvalid(() -> session.setMaxInactiveInterval(interval));
    }

    @Test
    public void getAttributeNames() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        Map<String, Object> attributes = Map.of("foo", UUID.randomUUID());
        Map<String, Object> context = mock(Map.class);

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(context).when(this.session).getContext();

        assertEquals(Set.of("foo"), session.getAttributeNames());

        verifyNoInteractions(context, this.closeTask);

        this.verifyWhenInvalid(session::getAttributeNames);
    }

    @Test
    public void getAttribute() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        String name = "foo";
        Object value = UUID.randomUUID();

        Map<String, Object> attributes = Map.of(name, value);
        Map<String, Object> context = mock(Map.class);

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(context).when(this.session).getContext();

        assertSame(value, session.getAttribute(name));
        assertNull(session.getAttribute("missing"));

        verifyNoInteractions(context, this.listener, this.closeTask);

        this.verifyWhenInvalid(() -> session.getAttribute(name));
    }

    @Test
    public void getAuthenticatedSessionAttribute() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        String name = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";

        Account account = mock(Account.class);
        AuthenticatedSession auth = new AuthenticatedSession(account, HttpServletRequest.FORM_AUTH);
        Map<String, Object> attributes = new TreeMap<>(Map.of(name, auth));
        Map<String, Object> context = new TreeMap<>();

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(context).when(this.session).getContext();

        AuthenticatedSession result = (AuthenticatedSession) session.getAttribute(name);

        assertSame(account, result.getAccount());
        assertSame(HttpServletRequest.FORM_AUTH, result.getMechanism());

        verifyNoInteractions(this.closeTask);

        AuthenticatedSession expected = new AuthenticatedSession(account, HttpServletRequest.BASIC_AUTH);
        context.put(name, expected);

        assertSame(expected, session.getAttribute(name));

        verifyNoInteractions(this.listener, this.closeTask);

        this.verifyWhenInvalid(() -> session.getAttribute(name));
    }

    @Test
    public void getWebSocketChannelsSessionAttribute() {
        this.getLocalContextSessionAttribute(UndertowSession.WEB_SOCKET_CHANNELS_ATTRIBUTE);
    }

    private void getLocalContextSessionAttribute(String name) {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        Map<String, Object> attributes = mock(Map.class);
        Object expected = new Object();
        Map<String, Object> context = Map.of(name, expected);

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(context).when(this.session).getContext();

        Object result = session.getAttribute(name);

        assertSame(expected, result);

        verify(attributes, never()).get(name);

        verifyNoInteractions(attributes, this.listener, this.closeTask);

        this.verifyWhenInvalid(() -> session.getAttribute(name));
    }

    @Test
    public void setAttribute() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        String name = "foo";
        Object expected = UUID.randomUUID();
        Object replacement = UUID.randomUUID();

        Map<String, Object> attributes = new TreeMap<>(Map.of(name, expected));
        Map<String, Object> context = mock(Map.class);

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(context).when(this.session).getContext();

        assertSame(expected, session.setAttribute(name, replacement));
        assertSame(replacement, attributes.get(name));

        verify(this.session).getAttributes();
        verify(this.listener, only()).attributeUpdated(session, name, replacement, expected);
        verifyNoInteractions(context, this.closeTask);

        this.verifyWhenInvalid(() -> session.setAttribute(name, UUID.randomUUID()));
    }

    @Test
    public void setNewAttribute() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        String name = "foo";
        Object value = UUID.randomUUID();

        Map<String, Object> attributes = new TreeMap<>();
        Map<String, Object> context = mock(Map.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(context).when(this.session).getContext();
        doReturn(listeners).when(this.manager).getSessionListeners();

        assertNull(session.setAttribute(name, value));
        assertSame(value, attributes.get(name));

        verify(this.session).getAttributes();
        verify(listener, only()).attributeAdded(session, name, value);
        verifyNoInteractions(context, this.closeTask);

        this.verifyWhenInvalid(() -> session.setAttribute(name, UUID.randomUUID()));
    }

    @Test
    public void setNullAttribute() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        String name = "name";
        Object value = UUID.randomUUID();

        Map<String, Object> attributes = new TreeMap<>(Map.of(name, value));
        Map<String, Object> context = mock(Map.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(context).when(this.session).getContext();
        doReturn(listeners).when(this.manager).getSessionListeners();

        assertSame(value, session.setAttribute(name, null));
        assertTrue(attributes.isEmpty());

        verify(this.session).getAttributes();
        verify(listener, only()).attributeRemoved(session, name, value);
        verifyNoInteractions(context, this.closeTask);

        this.verifyWhenInvalid(() -> session.setAttribute(name, UUID.randomUUID()));
    }

    @Test
    public void setSameAttribute() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        String name = "name";
        Object value = UUID.randomUUID();

        Map<String, Object> attributes = new TreeMap<>(Map.of(name, value));
        Map<String, Object> context = mock(Map.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(context).when(this.session).getContext();
        doReturn(listeners).when(this.manager).getSessionListeners();

        assertSame(value, session.setAttribute(name, value));

        verify(this.session).getAttributes();
        // Verify no listener invocations
        verifyNoInteractions(context, listener, this.closeTask);

        this.verifyWhenInvalid(() -> session.setAttribute(name, UUID.randomUUID()));
    }

    @Test
    public void setAuthenticatedSessionAttribute() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        // Verify non-auto-reauthenticating methods use session attributes
        String name = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";
        Account oldAccount = mock(Account.class);
        Account account = mock(Account.class);
        AuthenticatedSession oldAuth = new AuthenticatedSession(oldAccount, HttpServletRequest.FORM_AUTH);
        AuthenticatedSession auth = new AuthenticatedSession(account, HttpServletRequest.FORM_AUTH);

        Map<String, Object> attributes = new TreeMap<>(Map.of(name, oldAuth));
        Map<String, Object> context = new TreeMap<>();

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(context).when(this.session).getContext();

        Assert.assertSame(oldAuth, session.setAttribute(name, auth));
        Assert.assertSame(auth, attributes.get(name));
        Assert.assertTrue(context.isEmpty());

        // Verify this does not trigger application listeners
        verifyNoInteractions(this.listener, this.closeTask);

        Assert.assertSame(auth, session.setAttribute(name, null));
        Assert.assertTrue(attributes.isEmpty());
        Assert.assertTrue(context.isEmpty());

        verifyNoInteractions(this.listener, this.closeTask);

        // Verify auto-reauthenticating methods use session context
        auth = new AuthenticatedSession(account, HttpServletRequest.BASIC_AUTH);
        oldAuth = new AuthenticatedSession(oldAccount, HttpServletRequest.BASIC_AUTH);
        context.put(name, oldAuth);

        Assert.assertSame(oldAuth, session.setAttribute(name, auth));
        Assert.assertTrue(attributes.isEmpty());
        Assert.assertSame(auth, context.get(name));

        verifyNoInteractions(this.listener, this.closeTask);
    }

    @Test
    public void setWebSocketChannelsSessionAttribute() {
        this.setLocalContextSessionAttribute(UndertowSession.WEB_SOCKET_CHANNELS_ATTRIBUTE);
    }

    private void setLocalContextSessionAttribute(String name) {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        Object newValue = UUID.randomUUID();
        Object oldValue = UUID.randomUUID();

        Map<String, Object> attributes = mock(Map.class);
        Map<String, Object> context = new TreeMap<>(Map.of(name, oldValue));

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(context).when(this.session).getContext();

        assertSame(oldValue,  session.setAttribute(name, newValue));
        assertSame(newValue, context.get(name));

        verifyNoInteractions(attributes, this.listener, this.closeTask);

        this.verifyWhenInvalid(() -> session.setAttribute(name, UUID.randomUUID()));
    }

    @Test
    public void removeAttribute() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        String name = "name";
        Object value = UUID.randomUUID();

        Map<String, Object> attributes = new TreeMap<>(Map.of(name, value));
        Map<String, Object> context = mock(Map.class);

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(context).when(this.session).getContext();

        assertNull(session.removeAttribute("missing"));

        verifyNoInteractions(context, this.listener, this.closeTask);

        assertSame(value, session.removeAttribute(name));
        assertTrue(attributes.isEmpty());

        verify(this.listener, only()).attributeRemoved(session, name, value);
        verifyNoInteractions(context, this.closeTask);

        this.verifyWhenInvalid(() -> session.setAttribute(name, UUID.randomUUID()));
    }

    @Test
    public void removeAuthenticatedSessionAttribute() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        String name = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";

        Account oldAccount = mock(Account.class);
        AuthenticatedSession oldAuth = new AuthenticatedSession(oldAccount, HttpServletRequest.FORM_AUTH);
        Map<String, Object> attributes = new TreeMap<>(Map.of(name, oldAuth));
        Map<String, Object> context = new TreeMap<>();
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(context).when(this.session).getContext();

        assertSame(oldAuth, session.removeAttribute(name));

        verifyNoInteractions(this.listener, this.closeTask);

        assertNull(session.removeAttribute(name));

        verifyNoInteractions(this.listener, this.closeTask);

        oldAuth = new AuthenticatedSession(oldAccount, HttpServletRequest.BASIC_AUTH);
        context.put(name, oldAuth);

        assertSame(oldAuth, session.removeAttribute(name));
        assertTrue(context.isEmpty());

        verifyNoInteractions(this.listener, this.closeTask);

        assertNull(session.removeAttribute(name));

        verifyNoInteractions(this.listener, this.closeTask);

        this.verifyWhenInvalid(() -> session.removeAttribute(name));
    }

    @Test
    public void removeWebSocketChannelsSessionAttribute() {
        this.removeLocalContextSessionAttribute(UndertowSession.WEB_SOCKET_CHANNELS_ATTRIBUTE);
    }

    private void removeLocalContextSessionAttribute(String name) {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        Object oldValue = new Object();
        Map<String, Object> attributes = mock(Map.class);
        Map<String, Object> context = new TreeMap<>(Map.of(name, oldValue));

        doReturn(attributes).when(this.session).getAttributes();
        doReturn(context).when(this.session).getContext();

        assertSame(oldValue, session.removeAttribute(name));
        assertTrue(context.isEmpty());

        verifyNoInteractions(attributes, this.listener, this.closeTask);

        this.verifyWhenInvalid(() -> session.removeAttribute(name));
    }

    @Test
    public void invalidate() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        RecordableSessionManagerStatistics statistics = mock(RecordableSessionManagerStatistics.class);
        SessionConfig config = mock(SessionConfig.class);
        HttpServerExchange exchange = new HttpServerExchange(null);
        exchange.putAttachment(SessionConfig.ATTACHMENT_KEY, config);

        String sessionId = "session";
        String attributeName = "attribute";
        Object attributeValue = mock(HttpSessionActivationListener.class);
        Map<String, Object> attributes = Map.of(attributeName, attributeValue);
        Recordable<ImmutableSessionMetaData> recorder = mock(Recordable.class);

        doReturn(sessionId).when(this.session).getId();
        doReturn(attributes).when(this.session).getAttributes();
        doReturn(statistics).when(this.manager).getStatistics();
        doReturn(recorder).when(statistics).getInactiveSessionRecorder();

        InOrder order = inOrder(recorder, this.session, config, this.listener, this.closeTask);

        session.invalidate(exchange);

        order.verify(this.session).isValid();
        order.verify(this.listener).sessionDestroyed(session, exchange, SessionDestroyedReason.INVALIDATED);
        order.verify(this.listener).attributeRemoved(session, attributeName, attributeValue);
        order.verify(recorder).record(this.metaData);
        order.verify(this.session).invalidate();
        order.verify(config).clearSession(exchange, sessionId);
        order.verify(this.closeTask).accept(exchange);
        verifyNoMoreInteractions(this.listener);

        doReturn(false).when(this.session).isValid();

        assertThrows(IllegalStateException.class, () -> session.invalidate(exchange));

        verifyNoMoreInteractions(this.listener, recorder, config, this.closeTask);
    }

    @Test
    public void getSessionManager() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        assertSame(this.manager, session.getSessionManager());
    }

    @Test
    public void changeSessionId() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
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
        Instant end = Instant.now();
        Instant start = end.minus(Duration.ofSeconds(1L));
        Duration interval = Duration.ofSeconds(10L);

        doReturn(manager).when(this.manager).getSessionManager();
        doReturn(identifierFactory).when(manager).getIdentifierFactory();
        doReturn(newSessionId).when(identifierFactory).get();
        doReturn(newSession).when(manager).createSession(newSessionId);
        doReturn(oldAttributes).when(this.session).getAttributes();
        doReturn(oldMetaData).when(this.session).getMetaData();
        doReturn(newAttributes).when(newSession).getAttributes();
        doReturn(newMetaData).when(newSession).getMetaData();
        doReturn(Optional.of(Map.entry(start, end))).when(oldMetaData).getLastAccess();
        doReturn(Optional.of(interval)).when(oldMetaData).getMaxIdle();
        doReturn(oldSessionId).when(this.session).getId();
        doReturn(newSessionId).when(newSession).getId();
        doReturn(oldContext).when(this.session).getContext();
        doReturn(newContext).when(newSession).getContext();
        doReturn(listeners).when(this.manager).getSessionListeners();

        String result = session.changeSessionId(exchange, config);

        assertSame(newSessionId, result);

        verify(newMetaData).setLastAccess(Map.entry(start, end));
        verify(newMetaData).setMaxIdle(interval);
        verify(newAttributes).putAll(oldAttributes);
        verify(config).setSessionId(exchange, newSessionId);
        assertEquals(oldContext, newContext);
        verify(this.session).invalidate();
        verify(newSession, never()).invalidate();
        verify(listener).sessionIdChanged(session, oldSessionId);
    }

    @Test
    public void changeSessionIdImmortal() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
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
        Instant end = Instant.now();
        Instant start = end.minus(Duration.ofSeconds(1L));

        io.undertow.server.session.Session session = this.getSession(Optional.of(start));

        doReturn(manager).when(this.manager).getSessionManager();
        doReturn(identifierFactory).when(manager).getIdentifierFactory();
        doReturn(newSessionId).when(identifierFactory).get();
        doReturn(newSession).when(manager).createSession(newSessionId);
        doReturn(oldAttributes).when(this.session).getAttributes();
        doReturn(oldMetaData).when(this.session).getMetaData();
        doReturn(newAttributes).when(newSession).getAttributes();
        doReturn(newMetaData).when(newSession).getMetaData();
        doReturn(Optional.of(Map.entry(start, end))).when(oldMetaData).getLastAccess();
        doReturn(Optional.empty()).when(oldMetaData).getMaxIdle();
        doReturn(oldSessionId).when(this.session).getId();
        doReturn(newSessionId).when(newSession).getId();
        doReturn(oldContext).when(this.session).getContext();
        doReturn(newContext).when(newSession).getContext();
        doReturn(listeners).when(this.manager).getSessionListeners();

        String result = session.changeSessionId(exchange, config);

        assertSame(newSessionId, result);

        verify(newMetaData).setLastAccess(Map.entry(start, end));
        verify(newMetaData, never()).setMaxIdle(any());
        verify(newAttributes).putAll(oldAttributes);
        verify(config).setSessionId(exchange, newSessionId);
        assertEquals(oldContext, newContext);
        verify(this.session).invalidate();
        verify(newSession, never()).invalidate();
        verify(listener).sessionIdChanged(session, oldSessionId);
    }

    @Test
    public void changeSessionIdNew() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
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
        Duration interval = Duration.ofSeconds(10L);

        doReturn(manager).when(this.manager).getSessionManager();
        doReturn(identifierFactory).when(manager).getIdentifierFactory();
        doReturn(newSessionId).when(identifierFactory).get();
        doReturn(newSession).when(manager).createSession(newSessionId);
        doReturn(oldAttributes).when(this.session).getAttributes();
        doReturn(oldMetaData).when(this.session).getMetaData();
        doReturn(newAttributes).when(newSession).getAttributes();
        doReturn(newMetaData).when(newSession).getMetaData();
        doReturn(Optional.empty()).when(oldMetaData).getLastAccess();
        doReturn(Optional.of(interval)).when(oldMetaData).getMaxIdle();
        doReturn(oldSessionId).when(this.session).getId();
        doReturn(newSessionId).when(newSession).getId();
        doReturn(oldContext).when(this.session).getContext();
        doReturn(newContext).when(newSession).getContext();
        doReturn(listeners).when(this.manager).getSessionListeners();

        String result = session.changeSessionId(exchange, config);

        assertSame(newSessionId, result);

        verify(newMetaData, never()).setLastAccess(any());
        verify(newMetaData).setMaxIdle(interval);
        verify(newAttributes).putAll(oldAttributes);
        verify(config).setSessionId(exchange, newSessionId);
        assertEquals(oldContext, newContext);
        verify(this.session).invalidate();
        verify(newSession, never()).invalidate();
        verify(listener).sessionIdChanged(session, oldSessionId);
    }

    @Test
    public void changeSessionIdInvalid() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
        SessionManager<Map<String, Object>> manager = mock(SessionManager.class);
        String id = "current";
        String newId = "next";

        doReturn(false).when(this.session).isValid();
        doReturn(id).when(this.session).getId();
        doReturn(manager).when(this.manager).getSessionManager();
        doReturn(Supplier.of(newId)).when(manager).getIdentifierFactory();

        assertThrows(IllegalStateException.class, () -> session.changeSessionId(exchange, config));

        verify(this.session).isValid();
        verify(this.session).close();
        verify(this.closeTask, only()).accept(exchange);

        // Verify one-time close
        assertThrows(IllegalStateException.class, () -> session.changeSessionId(exchange, config));

        // Verify close was not called again
        verify(this.session).close();
        verifyNoMoreInteractions(this.closeTask);
    }

    public void changeSessionIdResponseCommitted() {
        io.undertow.server.session.Session session = this.getSession(Optional.empty());

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

    void verifyWhenInvalid(ThrowingRunnable operation) {

        doReturn(false).when(this.session).isValid();

        assertThrows(IllegalStateException.class, operation);

        verify(this.session, atLeastOnce()).isValid();
        verify(this.session).close();
        verify(this.closeTask, only()).accept(null);

        // Verify one-time close
        assertThrows(IllegalStateException.class, operation);

        // Verify close was not called again
        verify(this.session).close();
        verifyNoMoreInteractions(this.closeTask);
    }
}
