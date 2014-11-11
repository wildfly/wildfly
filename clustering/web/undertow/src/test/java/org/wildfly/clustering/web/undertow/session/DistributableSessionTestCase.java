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

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import org.wildfly.clustering.web.undertow.session.DistributableSession;
import org.wildfly.clustering.web.undertow.session.UndertowSessionManager;

public class DistributableSessionTestCase {
    private final UndertowSessionManager manager = mock(UndertowSessionManager.class);
    private final SessionConfig config = mock(SessionConfig.class);
    private final Session<LocalSessionContext> session = mock(Session.class);
    private final Batch batch = mock(Batch.class);

    private final io.undertow.server.session.Session adapter = new DistributableSession(this.manager, this.session, this.config, this.batch);
    
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

        when(this.batch.isActive()).thenReturn(true);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);

        this.adapter.requestDone(exchange);
        
        verify(this.session).close();
        verify(this.batch).close();
        verify(context).close();

        reset(this.batch, this.session, context);

        when(this.batch.isActive()).thenReturn(false);

        this.adapter.requestDone(exchange);

        verify(this.session, never()).close();
        verify(this.batch, never()).close();
        verify(context, never()).close();
    }
    
    @Test
    public void getCreationTime() {
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        Date date = new Date();
        
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getCreationTime()).thenReturn(date);
        
        long result = this.adapter.getCreationTime();
        
        assertEquals(date.getTime(), result);

        verify(context).close();
    }
    
    @Test
    public void getLastAccessedTime() {
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        Date date = new Date();
        
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getLastAccessedTime()).thenReturn(date);
        
        long result = this.adapter.getLastAccessedTime();
        
        assertEquals(date.getTime(), result);

        verify(context).close();
    }
    
    @Test
    public void getMaxInactiveInterval() {
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        long expected = 3600L;
        
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getMaxInactiveInterval(TimeUnit.SECONDS)).thenReturn(expected);
        
        long result = this.adapter.getMaxInactiveInterval();
        
        assertEquals(expected, result);

        verify(context).close();
    }
    
    @Test
    public void setMaxInactiveInterval() {
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        int interval = 3600;
        
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getMetaData()).thenReturn(metaData);
        
        this.adapter.setMaxInactiveInterval(interval);
        
        verify(metaData).setMaxInactiveInterval(interval, TimeUnit.SECONDS);

        verify(context).close();
    }
    
    @Test
    public void getAttributeNames() {
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
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        String name = "name";
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
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        String name = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";
        SessionAttributes attributes = mock(SessionAttributes.class);
        Account account = mock(Account.class);
        
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttribute(name)).thenReturn(account);
        
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
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        
        String name = "name";
        Integer value = Integer.valueOf(1);
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
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String name = "name";
        Integer value = Integer.valueOf(1);
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
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String name = "name";
        Object value = null;
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
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String name = "name";
        Integer value = Integer.valueOf(1);
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
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        String name = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";
        SessionAttributes attributes = mock(SessionAttributes.class);
        Account account = mock(Account.class);
        AuthenticatedSession session = new AuthenticatedSession(account, HttpServletRequest.FORM_AUTH);
        Account oldAccount = mock(Account.class);

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.setAttribute(name, account)).thenReturn(oldAccount);
        
        AuthenticatedSession result = (AuthenticatedSession) this.adapter.setAttribute(name, session);
        
        assertSame(oldAccount, result.getAccount());
        assertSame(HttpServletRequest.FORM_AUTH, result.getMechanism());
        
        verify(context).close();
        
        reset(context);
        
        when(attributes.setAttribute(name, account)).thenReturn(null);
        
        result = (AuthenticatedSession) this.adapter.setAttribute(name, session);
        
        assertNull(result);
        
        verify(context).close();
        
        reset(context);
        
        session = new AuthenticatedSession(account, HttpServletRequest.BASIC_AUTH);
        AuthenticatedSession oldSession = new AuthenticatedSession(oldAccount, HttpServletRequest.BASIC_AUTH);
        
        LocalSessionContext localContext = mock(LocalSessionContext.class);

        when(this.session.getLocalContext()).thenReturn(localContext);
        when(localContext.getAuthenticatedSession()).thenReturn(oldSession);
        
        result = (AuthenticatedSession) this.adapter.setAttribute(name, session);

        verify(localContext).setAuthenticatedSession(same(session));
        verify(context).close();
    }

    @Test
    public void removeAttribute() {
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String name = "name";
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
        SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
        Batcher<Batch> batcher = mock(Batcher.class);
        BatchContext context = mock(BatchContext.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String name = "name";

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
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        
        this.adapter.invalidate(exchange);
        
        verify(this.session).invalidate();
        verify(this.config).clearSession(exchange, sessionId);
        verify(listener).sessionDestroyed(this.adapter, exchange, SessionDestroyedReason.INVALIDATED);
        verify(this.batch).close();
        verify(context).close();
    }
    
    @Test
    public void getSessionManager() {
        assertSame(this.manager, this.adapter.getSessionManager());
    }
    
    @Test
    public void changeSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
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
        String sessionId = "session";
        String name = "name";
        Object value = new Object();
        Date date = new Date();
        long interval = 10L;
        AuthenticatedSession authenticatedSession = new AuthenticatedSession(null, null);
        ArgumentCaptor<TimeUnit> capturedUnit = ArgumentCaptor.forClass(TimeUnit.class);
        
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        when(batcher.resumeBatch(this.batch)).thenReturn(context);
        when(manager.createIdentifier()).thenReturn(sessionId);
        when(manager.createSession(sessionId)).thenReturn(session);
        when(this.session.getAttributes()).thenReturn(oldAttributes);
        when(this.session.getMetaData()).thenReturn(oldMetaData);
        when(session.getAttributes()).thenReturn(newAttributes);
        when(session.getMetaData()).thenReturn(newMetaData);
        when(oldAttributes.getAttributeNames()).thenReturn(Collections.singleton(name));
        when(oldAttributes.getAttribute(name)).thenReturn(value);
        when(newAttributes.setAttribute(name, value)).thenReturn(null);
        when(oldMetaData.getLastAccessedTime()).thenReturn(date);
        when(oldMetaData.getMaxInactiveInterval(capturedUnit.capture())).thenReturn(interval);
        when(session.getId()).thenReturn(sessionId);
        when(this.session.getLocalContext()).thenReturn(oldContext);
        when(session.getLocalContext()).thenReturn(newContext);
        when(oldContext.getAuthenticatedSession()).thenReturn(authenticatedSession);
        
        String result = this.adapter.changeSessionId(exchange, config);
        
        assertSame(sessionId, result);
        
        verify(newMetaData).setLastAccessedTime(date);
        verify(newMetaData).setMaxInactiveInterval(interval, capturedUnit.getValue());
        verify(config).setSessionId(exchange, sessionId);
        verify(newContext).setAuthenticatedSession(same(authenticatedSession));
        verify(context).close();
    }
}
