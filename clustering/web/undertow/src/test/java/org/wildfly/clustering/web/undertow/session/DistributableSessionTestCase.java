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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import javax.servlet.http.HttpServletRequest;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListener.SessionDestroyedReason;
import io.undertow.server.session.SessionListeners;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;
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
    private final UndertowSessionManager manager = mock(UndertowSessionManager.class);
    private final SessionConfig config = mock(SessionConfig.class);
    private final Session<LocalSessionContext> session = mock(Session.class);
    private final Batch batch = mock(Batch.class);
    private final Consumer<HttpServerExchange> closeTask = mock(Consumer.class);

    private final io.undertow.server.session.Session adapter = new DistributableSession(this.manager, this.session, this.config, this.batch, this.closeTask);

    @Test
    public void getId() {
        String id = "id";
        when(this.session.getId()).thenReturn(id);

        String result = this.adapter.getId();

        assertSame(id, result);
    }

    @Test
    public void requestDone() {
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        HttpServerExchange exchange = new HttpServerExchange(null);

        when(this.session.isValid()).thenReturn(true);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);

        this.adapter.requestDone(exchange);

        verify(this.session).close();
        verify(this.batch).close();
        verify(context).close();
        verify(this.closeTask).accept(exchange);

        reset(this.batch, this.session, context, this.closeTask);

        when(this.session.isValid()).thenReturn(false);

        this.adapter.requestDone(exchange);

        verify(this.session, never()).close();
        verify(this.batch, never()).close();
        verify(context, never()).close();
        verify(this.closeTask).accept(exchange);
    }

    @Test
    public void getCreationTime() {
        this.validate(session -> session.getCreationTime());

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        Instant now = Instant.now();

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getCreationTime()).thenReturn(now);

        long result = this.adapter.getCreationTime();

        assertEquals(now.toEpochMilli(), result);

        verify(context).close();
    }

    @Test
    public void getLastAccessedTime() {
        this.validate(session -> session.getLastAccessedTime());

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        Instant now = Instant.now();

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getLastAccessedTime()).thenReturn(now);

        long result = this.adapter.getLastAccessedTime();

        assertEquals(now.toEpochMilli(), result);

        verify(context).close();
    }

    @Test
    public void getMaxInactiveInterval() {
        this.validate(session -> session.getMaxInactiveInterval());

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        long expected = 3600L;

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getMaxInactiveInterval()).thenReturn(Duration.ofSeconds(expected));

        long result = this.adapter.getMaxInactiveInterval();

        assertEquals(expected, result);

        verify(context).close();
    }

    @Test
    public void setMaxInactiveInterval() {
        int interval = 3600;
        this.validate(session -> session.setMaxInactiveInterval(interval));

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getMetaData()).thenReturn(metaData);

        this.adapter.setMaxInactiveInterval(interval);

        verify(metaData).setMaxInactiveInterval(Duration.ofSeconds(interval));

        verify(context).close();
    }

    @Test
    public void getAttributeNames() {
        this.validate(session -> session.getAttributeNames());

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Set<String> expected = Collections.singleton("name");

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttributeNames()).thenReturn(expected);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);

        Object result = this.adapter.getAttributeNames();

        assertSame(expected, result);

        verify(context).close();
    }

    @Test
    public void getAttribute() {
        String name = "name";
        this.validate(session -> session.getAttribute(name));

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Object expected = new Object();

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttribute(name)).thenReturn(expected);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);

        Object result = this.adapter.getAttribute(name);

        assertSame(expected, result);

        verify(context).close();
    }

    @Test
    public void getAuthenticatedSessionAttribute() {
        String name = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";
        this.validate(session -> session.getAttribute(name));

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
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

        AuthenticatedSession result = (AuthenticatedSession) this.adapter.getAttribute(name);

        assertSame(account, result.getAccount());
        assertSame(HttpServletRequest.FORM_AUTH, result.getMechanism());

        verify(context).close();

        reset(context);

        LocalSessionContext localContext = mock(LocalSessionContext.class);
        AuthenticatedSession expected = new AuthenticatedSession(account, HttpServletRequest.BASIC_AUTH);

        when(attributes.getAttribute(name)).thenReturn(null);
        when(this.session.getLocalContext()).thenReturn(localContext);
        when(localContext.getAuthenticatedSession()).thenReturn(expected);

        result = (AuthenticatedSession) this.adapter.getAttribute(name);

        assertSame(expected, result);

        verify(context).close();
    }

    @Test
    public void setAttribute() {
        String name = "name";
        Integer value = Integer.valueOf(1);
        this.validate(session -> session.setAttribute(name, value));

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
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

        Object result = this.adapter.setAttribute(name, value);

        assertSame(expected, result);

        verify(listener, never()).attributeAdded(this.adapter, name, value);
        verify(listener).attributeUpdated(this.adapter, name, value, expected);
        verify(listener, never()).attributeRemoved(same(this.adapter), same(name), any());
        verify(context).close();
    }

    @Test
    public void setNewAttribute() {
        String name = "name";
        Integer value = Integer.valueOf(1);
        this.validate(session -> session.setAttribute(name, value));

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
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

        Object result = this.adapter.setAttribute(name, value);

        assertSame(expected, result);

        verify(listener).attributeAdded(this.adapter, name, value);
        verify(listener, never()).attributeUpdated(same(this.adapter), same(name), same(value), any());
        verify(listener, never()).attributeRemoved(same(this.adapter), same(name), any());
        verify(context).close();
    }

    @Test
    public void setNullAttribute() {
        String name = "name";
        Object value = null;
        this.validate(session -> session.setAttribute(name, value));

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
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

        Object result = this.adapter.setAttribute(name, value);

        assertSame(expected, result);

        verify(listener, never()).attributeAdded(this.adapter, name, value);
        verify(listener, never()).attributeUpdated(same(this.adapter), same(name), same(value), any());
        verify(listener).attributeRemoved(this.adapter, name, expected);
        verify(context).close();
    }

    @Test
    public void setSameAttribute() {
        String name = "name";
        Integer value = Integer.valueOf(1);
        this.validate(session -> session.setAttribute(name, value));

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
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

        Object result = this.adapter.setAttribute(name, value);

        assertSame(expected, result);

        verify(listener, never()).attributeAdded(this.adapter, name, value);
        verify(listener, never()).attributeUpdated(same(this.adapter), same(name), same(value), any());
        verify(listener, never()).attributeRemoved(same(this.adapter), same(name), any());
        verify(context).close();
    }

    @Test
    public void setAuthenticatedSessionAttribute() {
        String name = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";
        Account account = mock(Account.class);
        AuthenticatedSession auth = new AuthenticatedSession(account, HttpServletRequest.FORM_AUTH);
        this.validate(session -> session.setAttribute(name, "bar"));

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
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

        AuthenticatedSession result = (AuthenticatedSession) this.adapter.setAttribute(name, auth);

        assertSame(auth.getAccount(), capturedAuth.getValue().getAccount());
        assertSame(auth.getMechanism(), capturedAuth.getValue().getMechanism());

        assertSame(oldAccount, result.getAccount());
        assertSame(HttpServletRequest.FORM_AUTH, result.getMechanism());

        verify(context).close();

        reset(context, attributes);

        capturedAuth = ArgumentCaptor.forClass(AuthenticatedSession.class);

        when(attributes.setAttribute(same(name), capturedAuth.capture())).thenReturn(null);

        result = (AuthenticatedSession) this.adapter.setAttribute(name, auth);

        assertSame(auth.getAccount(), capturedAuth.getValue().getAccount());
        assertSame(auth.getMechanism(), capturedAuth.getValue().getMechanism());

        assertNull(result);

        verify(context).close();

        reset(context, attributes);

        auth = new AuthenticatedSession(account, HttpServletRequest.BASIC_AUTH);
        AuthenticatedSession oldSession = new AuthenticatedSession(oldAccount, HttpServletRequest.BASIC_AUTH);

        LocalSessionContext localContext = mock(LocalSessionContext.class);

        when(this.session.getLocalContext()).thenReturn(localContext);
        when(localContext.getAuthenticatedSession()).thenReturn(oldSession);

        result = (AuthenticatedSession) this.adapter.setAttribute(name, auth);

        verify(localContext).setAuthenticatedSession(same(auth));
        verify(context).close();
    }

    @Test
    public void removeAttribute() {
        String name = "name";
        this.validate(session -> session.removeAttribute(name));

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
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

        Object result = this.adapter.removeAttribute(name);

        assertSame(expected, result);

        verify(listener).attributeRemoved(this.adapter, name, expected);
        verify(context).close();
    }

    @Test
    public void removeNonExistingAttribute() {
        String name = "name";
        this.validate(session -> session.removeAttribute(name));

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
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

        Object result = this.adapter.removeAttribute(name);

        assertNull(result);

        verify(listener, never()).attributeRemoved(same(this.adapter), same(name), any());
        verify(context).close();
    }

    @Test
    public void invalidate() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        this.validate(exchange, session -> session.invalidate(exchange));

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
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

        this.adapter.invalidate(exchange);

        verify(this.session).invalidate();
        verify(this.config).clearSession(exchange, sessionId);
        verify(listener).sessionDestroyed(this.adapter, exchange, SessionDestroyedReason.INVALIDATED);
        verify(this.batch).close();
        verify(context).close();
        verify(this.closeTask).accept(exchange);
    }

    @Test
    public void getSessionManager() {
        assertSame(this.manager, this.adapter.getSessionManager());
    }

    @Test
    public void changeSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
        this.validate(exchange, session -> session.changeSessionId(exchange, config));

        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        Session<LocalSessionContext> session = mock(Session.class);
        SessionAttributes oldAttributes = mock(SessionAttributes.class);
        SessionAttributes newAttributes = mock(SessionAttributes.class);
        SessionMetaData oldMetaData = mock(SessionMetaData.class);
        SessionMetaData newMetaData = mock(SessionMetaData.class);
        LocalSessionContext oldContext = mock(LocalSessionContext.class);
        LocalSessionContext newContext = mock(LocalSessionContext.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String oldSessionId = "old";
        String newSessionId = "new";
        String name = "name";
        Object value = new Object();
        Instant now = Instant.now();
        Duration interval = Duration.ofSeconds(10L);
        AuthenticatedSession authenticatedSession = new AuthenticatedSession(null, null);

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(manager.createIdentifier()).thenReturn(newSessionId);
        when(manager.createSession(newSessionId)).thenReturn(session);
        when(this.session.getAttributes()).thenReturn(oldAttributes);
        when(this.session.getMetaData()).thenReturn(oldMetaData);
        when(session.getAttributes()).thenReturn(newAttributes);
        when(session.getMetaData()).thenReturn(newMetaData);
        when(oldAttributes.getAttributeNames()).thenReturn(Collections.singleton(name));
        when(oldAttributes.getAttribute(name)).thenReturn(value);
        when(newAttributes.setAttribute(name, value)).thenReturn(null);
        when(oldMetaData.getLastAccessedTime()).thenReturn(now);
        when(oldMetaData.getMaxInactiveInterval()).thenReturn(interval);
        when(this.session.getId()).thenReturn(oldSessionId);
        when(session.getId()).thenReturn(newSessionId);
        when(this.session.getLocalContext()).thenReturn(oldContext);
        when(session.getLocalContext()).thenReturn(newContext);
        when(oldContext.getAuthenticatedSession()).thenReturn(authenticatedSession);
        when(this.manager.getSessionListeners()).thenReturn(listeners);

        String result = this.adapter.changeSessionId(exchange, config);

        assertSame(newSessionId, result);

        verify(newMetaData).setLastAccessedTime(now);
        verify(newMetaData).setMaxInactiveInterval(interval);
        verify(config).setSessionId(exchange, newSessionId);
        verify(newContext).setAuthenticatedSession(same(authenticatedSession));
        verify(listener).sessionIdChanged(this.adapter, oldSessionId);
        verify(context).close();
    }


    private <R> void validate(Consumer<io.undertow.server.session.Session> consumer) {
        this.validate(null, consumer);
    }

    @SuppressWarnings("unchecked")
    private <R> void validate(HttpServerExchange exchange, Consumer<io.undertow.server.session.Session> consumer) {
        when(this.session.isValid()).thenReturn(false, true);

        try {
            consumer.accept(this.adapter);
            fail("Invalid session should throw IllegalStateException");
        } catch (IllegalStateException e) {
            verify(this.closeTask).accept(exchange);
            reset(this.closeTask);
        }
    }
}
