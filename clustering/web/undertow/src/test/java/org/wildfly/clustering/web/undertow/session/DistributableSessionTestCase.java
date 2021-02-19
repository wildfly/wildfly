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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionActivationListener;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListener.SessionDestroyedReason;
import io.undertow.server.session.SessionListeners;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionMetaData;

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
    private final Batch batch = mock(Batch.class);
    private final Consumer<HttpServerExchange> closeTask = mock(Consumer.class);

    @Test
    public void getId() {
        String id = "id";

        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        when(this.session.getId()).thenReturn(id);

        String result = session.getId();

        assertSame(id, result);
    }

    @Test
    public void requestDone() {
        Instant creationTime = Instant.now();
        // New session
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(true);
        when(this.metaData.getCreationTime()).thenReturn(creationTime);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        HttpServerExchange exchange = new HttpServerExchange(null);
        ArgumentCaptor<Instant> capturedLastAccessStartTime = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> capturedLastAccessEndTime = ArgumentCaptor.forClass(Instant.class);

        when(this.session.isValid()).thenReturn(true);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        doNothing().when(this.metaData).setLastAccess(capturedLastAccessStartTime.capture(), capturedLastAccessEndTime.capture());

        session.requestDone(exchange);

        Instant lastAccessStartTime = capturedLastAccessStartTime.getValue();
        Instant lastAccessEndTime = capturedLastAccessEndTime.getValue();

        Assert.assertSame(creationTime, lastAccessStartTime);
        Assert.assertNotSame(creationTime, lastAccessEndTime);
        Assert.assertFalse(lastAccessStartTime.isAfter(lastAccessEndTime));

        verify(this.session).close();
        verify(this.batch).close();
        verify(context).close();
        verify(this.closeTask).accept(exchange);

        reset(this.batch, this.session, this.metaData, context, this.closeTask);

        capturedLastAccessStartTime = ArgumentCaptor.forClass(Instant.class);
        capturedLastAccessEndTime = ArgumentCaptor.forClass(Instant.class);

        // Existing session
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        when(this.session.isValid()).thenReturn(true);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        doNothing().when(this.metaData).setLastAccess(capturedLastAccessStartTime.capture(), capturedLastAccessEndTime.capture());

        session.requestDone(exchange);

        lastAccessStartTime = capturedLastAccessStartTime.getValue();
        lastAccessEndTime = capturedLastAccessEndTime.getValue();

        Assert.assertNotSame(lastAccessStartTime, lastAccessEndTime);
        Assert.assertFalse(lastAccessStartTime.isAfter(lastAccessEndTime));

        verify(this.metaData).setLastAccess(any(Instant.class), any(Instant.class));
        verify(this.session).close();
        verify(this.batch).close();
        verify(context).close();
        verify(this.closeTask).accept(exchange);

        reset(this.batch, this.session, this.metaData, context, this.closeTask);

        // Invalid session, closed batch
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(true);

        session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        when(this.session.isValid()).thenReturn(false);
        when(this.batch.getState()).thenReturn(Batch.State.CLOSED);

        session.requestDone(exchange);

        verify(this.session, never()).close();
        verify(this.batch, never()).close();
        verify(context, never()).close();
        verify(this.closeTask).accept(exchange);

        reset(this.batch, this.session, this.metaData, context, this.closeTask);

        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(true);

        session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        // Invalid session, active batch
        when(this.session.isValid()).thenReturn(false);
        when(this.batch.getState()).thenReturn(Batch.State.ACTIVE);

        session.requestDone(exchange);

        verify(this.session, never()).close();
        verify(this.batch).close();
        verify(context).close();
        verify(this.closeTask).accept(exchange);
    }

    @Test
    public void getCreationTime() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        this.validate(session, io.undertow.server.session.Session::getCreationTime);

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        Instant now = Instant.now();

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getCreationTime()).thenReturn(now);

        long result = session.getCreationTime();

        assertEquals(now.toEpochMilli(), result);

        verify(context).close();
    }

    @Test
    public void getLastAccessedTime() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        this.validate(session, io.undertow.server.session.Session::getLastAccessedTime);

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        Instant now = Instant.now();

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getLastAccessStartTime()).thenReturn(now);

        long result = session.getLastAccessedTime();

        assertEquals(now.toEpochMilli(), result);

        verify(context).close();
    }

    @Test
    public void getMaxInactiveInterval() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        this.validate(session, io.undertow.server.session.Session::getMaxInactiveInterval);

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        long expected = 3600L;

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getMaxInactiveInterval()).thenReturn(Duration.ofSeconds(expected));

        long result = session.getMaxInactiveInterval();

        assertEquals(expected, result);

        verify(context).close();
    }

    @Test
    public void setMaxInactiveInterval() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        int interval = 3600;
        this.validate(session, s -> s.setMaxInactiveInterval(interval));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getMetaData()).thenReturn(metaData);

        session.setMaxInactiveInterval(interval);

        verify(metaData).setMaxInactiveInterval(Duration.ofSeconds(interval));

        verify(context).close();
    }

    @Test
    public void getAttributeNames() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        this.validate(session, io.undertow.server.session.Session::getAttributeNames);

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Set<String> expected = Collections.singleton("name");

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttributeNames()).thenReturn(expected);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);

        Object result = session.getAttributeNames();

        assertSame(expected, result);

        verify(context).close();
    }

    @Test
    public void getAttribute() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        String name = "name";
        this.validate(session, s -> s.getAttribute(name));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Object expected = new Object();

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttribute(name)).thenReturn(expected);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);

        Object result = session.getAttribute(name);

        assertSame(expected, result);

        verify(context).close();
    }

    @Test
    public void getAuthenticatedSessionAttribute() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        String name = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";
        this.validate(session, s -> s.getAttribute(name));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Account account = mock(Account.class);
        AuthenticatedSession auth = new AuthenticatedSession(account, HttpServletRequest.FORM_AUTH);

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttribute(name)).thenReturn(auth);

        AuthenticatedSession result = (AuthenticatedSession) session.getAttribute(name);

        assertSame(account, result.getAccount());
        assertSame(HttpServletRequest.FORM_AUTH, result.getMechanism());

        verify(context).close();

        reset(context);

        AuthenticatedSession expected = new AuthenticatedSession(account, HttpServletRequest.BASIC_AUTH);
        Map<String, Object> localContext = Collections.singletonMap(name, expected);

        when(attributes.getAttribute(name)).thenReturn(null);
        when(this.session.getLocalContext()).thenReturn(localContext);

        result = (AuthenticatedSession) session.getAttribute(name);

        assertSame(expected, result);

        verify(context).close();
    }

    @Test
    public void getWebSocketChannelsSessionAttribute() {
        this.getLocalContextSessionAttribute(DistributableSession.WEB_SOCKET_CHANNELS_ATTRIBUTE);
    }

    @Test
    public void getIdentityContainerSessionAttribute() {
        this.getLocalContextSessionAttribute(DistributableSession.IDENTITY_CONTAINER_ATTRIBUTE);
    }

    private void getLocalContextSessionAttribute(String name) {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        this.validate(session, s -> s.getAttribute(name));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        Object expected = new Object();
        Map<String, Object> localContext = Collections.singletonMap(name, expected);

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getLocalContext()).thenReturn(localContext);

        Object result = session.getAttribute(name);

        assertSame(expected, result);

        verify(attributes, never()).getAttribute(name);
        verify(context).close();
    }

    @Test
    public void setAttribute() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        String name = "name";
        Integer value = Integer.valueOf(1);
        this.validate(session, s -> s.setAttribute(name, value));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);

        Object expected = new Object();

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.setAttribute(name, value)).thenReturn(expected);
        when(this.manager.getSessionListeners()).thenReturn(listeners);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);

        Object result = session.setAttribute(name, value);

        assertSame(expected, result);

        verify(listener, never()).attributeAdded(session, name, value);
        verify(listener).attributeUpdated(session, name, value, expected);
        verify(listener, never()).attributeRemoved(same(session), same(name), any());
        verify(context).close();
    }

    @Test
    public void setNewAttribute() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        String name = "name";
        Integer value = Integer.valueOf(1);
        this.validate(session, s -> s.setAttribute(name, value));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        Object expected = null;

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.setAttribute(name, value)).thenReturn(expected);
        when(this.manager.getSessionListeners()).thenReturn(listeners);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);

        Object result = session.setAttribute(name, value);

        assertSame(expected, result);

        verify(listener).attributeAdded(session, name, value);
        verify(listener, never()).attributeUpdated(same(session), same(name), same(value), any());
        verify(listener, never()).attributeRemoved(same(session), same(name), any());
        verify(context).close();
    }

    @Test
    public void setNullAttribute() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        String name = "name";
        Object value = null;
        this.validate(session, s -> s.setAttribute(name, value));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        Object expected = new Object();

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.removeAttribute(name)).thenReturn(expected);
        when(this.manager.getSessionListeners()).thenReturn(listeners);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);

        Object result = session.setAttribute(name, value);

        assertSame(expected, result);

        verify(listener, never()).attributeAdded(session, name, value);
        verify(listener, never()).attributeUpdated(same(session), same(name), same(value), any());
        verify(listener).attributeRemoved(session, name, expected);
        verify(context).close();
    }

    @Test
    public void setSameAttribute() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        String name = "name";
        Integer value = Integer.valueOf(1);
        this.validate(session, s -> s.setAttribute(name, value));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        Object expected = value;

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.setAttribute(name, value)).thenReturn(expected);
        when(this.manager.getSessionListeners()).thenReturn(listeners);

        Object result = session.setAttribute(name, value);

        assertSame(expected, result);

        verify(listener, never()).attributeAdded(session, name, value);
        verify(listener, never()).attributeUpdated(same(session), same(name), same(value), any());
        verify(listener, never()).attributeRemoved(same(session), same(name), any());
        verify(context).close();
    }

    @Test
    public void setAuthenticatedSessionAttribute() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        String name = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";
        Account account = mock(Account.class);
        AuthenticatedSession auth = new AuthenticatedSession(account, HttpServletRequest.FORM_AUTH);
        this.validate(session, s -> s.setAttribute(name, "bar"));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Account oldAccount = mock(Account.class);
        AuthenticatedSession oldAuth = new AuthenticatedSession(oldAccount, HttpServletRequest.FORM_AUTH);
        ArgumentCaptor<AuthenticatedSession> capturedAuth = ArgumentCaptor.forClass(AuthenticatedSession.class);

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.setAttribute(same(name), capturedAuth.capture())).thenReturn(oldAuth);

        AuthenticatedSession result = (AuthenticatedSession) session.setAttribute(name, auth);

        assertSame(auth.getAccount(), capturedAuth.getValue().getAccount());
        assertSame(auth.getMechanism(), capturedAuth.getValue().getMechanism());

        assertSame(oldAccount, result.getAccount());
        assertSame(HttpServletRequest.FORM_AUTH, result.getMechanism());

        verify(context).close();

        reset(context, attributes);

        capturedAuth = ArgumentCaptor.forClass(AuthenticatedSession.class);

        when(attributes.setAttribute(same(name), capturedAuth.capture())).thenReturn(null);

        result = (AuthenticatedSession) session.setAttribute(name, auth);

        assertSame(auth.getAccount(), capturedAuth.getValue().getAccount());
        assertSame(auth.getMechanism(), capturedAuth.getValue().getMechanism());

        assertNull(result);

        verify(context).close();

        reset(context, attributes);

        auth = new AuthenticatedSession(account, HttpServletRequest.BASIC_AUTH);
        AuthenticatedSession oldSession = new AuthenticatedSession(oldAccount, HttpServletRequest.BASIC_AUTH);

        Map<String, Object> localContext = new HashMap<>();
        localContext.put(name, oldSession);

        when(this.session.getLocalContext()).thenReturn(localContext);

        result = (AuthenticatedSession) session.setAttribute(name, auth);

        assertSame(auth, localContext.get(name));
        verify(context).close();
    }

    @Test
    public void setWebSocketChannelsSessionAttribute() {
        this.setLocalContextSessionAttribute(DistributableSession.WEB_SOCKET_CHANNELS_ATTRIBUTE);
    }

    @Test
    public void setIdentityContainerSessionAttribute() {
        this.setLocalContextSessionAttribute(DistributableSession.IDENTITY_CONTAINER_ATTRIBUTE);
    }

    private void setLocalContextSessionAttribute(String name) {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        Object newValue = new Object();
        Object oldValue = new Object();
        this.validate(session, s -> s.setAttribute(name, "bar"));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        Map<String, Object> localContext = new HashMap<>();
        localContext.put(name, oldValue);

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getLocalContext()).thenReturn(localContext);

        Object result = session.setAttribute(name, newValue);

        assertSame(oldValue, result);

        assertSame(newValue, localContext.get(name));
        verify(attributes, never()).setAttribute(name, newValue);
        verify(context).close();
    }

    @Test
    public void removeAttribute() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        String name = "name";
        this.validate(session, s -> s.removeAttribute(name));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        Object expected = new Object();

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.removeAttribute(name)).thenReturn(expected);
        when(this.manager.getSessionListeners()).thenReturn(listeners);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);

        Object result = session.removeAttribute(name);

        assertSame(expected, result);

        verify(listener).attributeRemoved(session, name, expected);
        verify(context).close();
    }

    @Test
    public void removeNonExistingAttribute() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        String name = "name";
        this.validate(session, s -> s.removeAttribute(name));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.removeAttribute(name)).thenReturn(null);
        when(this.manager.getSessionListeners()).thenReturn(listeners);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);

        Object result = session.removeAttribute(name);

        assertNull(result);

        verify(listener, never()).attributeRemoved(same(session), same(name), any());
        verify(context).close();
    }

    @Test
    public void removeAuthenticatedSessionAttribute() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        String name = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";
        this.validate(session, s -> s.removeAttribute(name));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Account oldAccount = mock(Account.class);
        AuthenticatedSession oldAuth = new AuthenticatedSession(oldAccount, HttpServletRequest.FORM_AUTH);

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.removeAttribute(same(name))).thenReturn(oldAuth);

        AuthenticatedSession result = (AuthenticatedSession) session.removeAttribute(name);

        assertSame(oldAccount, result.getAccount());
        assertSame(HttpServletRequest.FORM_AUTH, result.getMechanism());

        verify(context).close();

        reset(context, attributes);

        Map<String, Object> localContext = new HashMap<>();
        AuthenticatedSession oldSession = new AuthenticatedSession(oldAccount, HttpServletRequest.BASIC_AUTH);
        localContext.put(name, oldSession);
        when(attributes.removeAttribute(same(name))).thenReturn(null);
        when(this.session.getLocalContext()).thenReturn(localContext);

        result = (AuthenticatedSession) session.removeAttribute(name);

        assertSame(result, oldSession);
        assertNull(localContext.get(name));
        verify(context).close();

        reset(context, attributes);

        result = (AuthenticatedSession) session.removeAttribute(name);

        assertNull(result);

        verify(context).close();
    }

    @Test
    public void removeWebSocketChannelsSessionAttribute() {
        this.removeLocalContextSessionAttribute(DistributableSession.WEB_SOCKET_CHANNELS_ATTRIBUTE);
    }

    @Test
    public void removeIdentityContainerSessionAttribute() {
        this.removeLocalContextSessionAttribute(DistributableSession.IDENTITY_CONTAINER_ATTRIBUTE);
    }

    private void removeLocalContextSessionAttribute(String name) {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        Object oldValue = new Object();
        this.validate(session, s -> s.setAttribute(name, "bar"));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        Map<String, Object> localContext = new HashMap<>();
        localContext.put(name, oldValue);

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getLocalContext()).thenReturn(localContext);

        Object result = session.removeAttribute(name);

        assertSame(oldValue, result);

        assertNull(localContext.get(name));
        verify(attributes, never()).removeAttribute(name);
        verify(context).close();
    }

    @Test
    public void invalidate() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        HttpServerExchange exchange = new HttpServerExchange(null);
        this.validate(session, exchange, s -> s.invalidate(exchange));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionListener listener = mock(SessionListener.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String sessionId = "session";
        String attributeName = "attribute";
        Object attributeValue = mock(HttpSessionActivationListener.class);

        when(this.manager.getSessionListeners()).thenReturn(listeners);
        when(this.session.getId()).thenReturn(sessionId);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.batch.getState()).thenReturn(Batch.State.ACTIVE);
        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttributeNames()).thenReturn(Collections.singleton("attribute"));
        when(attributes.getAttribute(attributeName)).thenReturn(attributeValue);

        session.invalidate(exchange);

        verify(this.session).invalidate();
        verify(this.config).clearSession(exchange, sessionId);
        verify(listener).sessionDestroyed(session, exchange, SessionDestroyedReason.INVALIDATED);
        verify(listener).attributeRemoved(session, attributeName, attributeValue);
        verify(this.batch).close();
        verify(context).close();
        verify(this.closeTask).accept(exchange);
    }

    @Test
    public void invalidateWhenResponseDone() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        HttpServerExchange exchange = new HttpServerExchange(null);
        this.validate(session, exchange, s -> s.invalidate(exchange));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String sessionId = "session";

        when(this.manager.getSessionListeners()).thenReturn(listeners);
        when(this.session.getId()).thenReturn(sessionId);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.batch.getState()).thenReturn(Batch.State.CLOSED);

        Assert.assertThrows(IllegalStateException.class, () -> session.invalidate(exchange));
    }

    @Test
    public void getSessionManager() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        assertSame(this.manager, session.getSessionManager());
    }

    @Test
    public void changeSessionId() {
        when(this.session.getMetaData()).thenReturn(this.metaData);
        when(this.metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session session = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
        this.validate(session, exchange, s -> s.changeSessionId(exchange, config));

        SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        Session<Map<String, Object>> newSession = mock(Session.class);
        SessionAttributes oldAttributes = mock(SessionAttributes.class);
        SessionAttributes newAttributes = mock(SessionAttributes.class);
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
        String name = "name";
        Object value = new Object();
        Instant now = Instant.now();
        Duration interval = Duration.ofSeconds(10L);

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(manager.createIdentifier()).thenReturn(newSessionId);
        when(manager.createSession(newSessionId)).thenReturn(newSession);
        when(this.session.getAttributes()).thenReturn(oldAttributes);
        when(this.session.getMetaData()).thenReturn(oldMetaData);
        when(newSession.getAttributes()).thenReturn(newAttributes);
        when(newSession.getMetaData()).thenReturn(newMetaData);
        when(oldAttributes.getAttributeNames()).thenReturn(Collections.singleton(name));
        when(oldAttributes.getAttribute(name)).thenReturn(value);
        when(newAttributes.setAttribute(name, value)).thenReturn(null);
        when(oldMetaData.getLastAccessStartTime()).thenReturn(now);
        when(oldMetaData.getLastAccessEndTime()).thenReturn(now);
        when(oldMetaData.getMaxInactiveInterval()).thenReturn(interval);
        when(this.session.getId()).thenReturn(oldSessionId);
        when(newSession.getId()).thenReturn(newSessionId);
        when(this.session.getLocalContext()).thenReturn(oldContext);
        when(newSession.getLocalContext()).thenReturn(newContext);
        when(this.manager.getSessionListeners()).thenReturn(listeners);

        String result = session.changeSessionId(exchange, config);

        assertSame(newSessionId, result);

        verify(newMetaData).setLastAccess(now, now);
        verify(newMetaData).setMaxInactiveInterval(interval);
        verify(config).setSessionId(exchange, newSessionId);
        assertEquals(oldContext, newContext);
        verify(listener).sessionIdChanged(session, oldSessionId);
        verify(context).close();
    }


    private <R> void validate(io.undertow.server.session.Session session, Consumer<io.undertow.server.session.Session> consumer) {
        this.validate(session, null, consumer);
    }

    @SuppressWarnings("unchecked")
    private <R> void validate(io.undertow.server.session.Session session, HttpServerExchange exchange, Consumer<io.undertow.server.session.Session> consumer) {
        when(this.session.isValid()).thenReturn(false, true);

        assertThrows(IllegalStateException.class, () -> consumer.accept(session));

        verify(this.closeTask).accept(exchange);
        reset(this.closeTask);
    }
}
