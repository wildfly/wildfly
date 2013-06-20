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
import java.util.concurrent.TimeUnit;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListener.SessionDestroyedReason;
import io.undertow.server.session.SessionListeners;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionMetaData;
import org.wildfly.clustering.web.undertow.session.SessionFacade;
import org.wildfly.clustering.web.undertow.session.UndertowSessionManager;

public class SessionFacadeTestCase {
    private final UndertowSessionManager manager = mock(UndertowSessionManager.class);
    private final SessionConfig config = mock(SessionConfig.class);
    private final Session<Void> session = mock(Session.class);
    
    private final io.undertow.server.session.Session facade = new SessionFacade(this.manager, this.session, this.config);
    
    @Test
    public void getId() {
        String id = "id";
        when(this.session.getId()).thenReturn(id);
        
        String result = this.facade.getId();
        
        assertSame(id, result);
    }
    
    @Test
    public void requestDone() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionManager<Void> manager = mock(SessionManager.class);
        Batcher batcher = mock(Batcher.class);

        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);

        this.facade.requestDone(exchange);
        
        verify(this.session).close();
        verify(batcher).endBatch(true);
    }
    
    @Test
    public void getCreationTime() {
        SessionMetaData metaData = mock(SessionMetaData.class);
        Date date = new Date();
        
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getCreationTime()).thenReturn(date);
        
        long result = this.facade.getCreationTime();
        
        assertEquals(date.getTime(), result);
    }
    
    @Test
    public void getLastAccessedTime() {
        SessionMetaData metaData = mock(SessionMetaData.class);
        Date date = new Date();
        
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getLastAccessedTime()).thenReturn(date);
        
        long result = this.facade.getLastAccessedTime();
        
        assertEquals(date.getTime(), result);
    }
    
    @Test
    public void getMaxInactiveInterval() {
        SessionMetaData metaData = mock(SessionMetaData.class);
        long expected = 3600L;
        
        when(this.session.getMetaData()).thenReturn(metaData);
        when(metaData.getMaxInactiveInterval(TimeUnit.SECONDS)).thenReturn(expected);
        
        long result = this.facade.getMaxInactiveInterval();
        
        assertEquals(expected, result);
    }
    
    @Test
    public void setMaxInactiveInterval() {
        SessionMetaData metaData = mock(SessionMetaData.class);
        int interval = 3600;
        
        when(this.session.getMetaData()).thenReturn(metaData);
        
        this.facade.setMaxInactiveInterval(interval);
        
        verify(metaData).setMaxInactiveInterval(interval, TimeUnit.SECONDS);
    }
    
    @Test
    public void getAttribute() {
        SessionAttributes attributes = mock(SessionAttributes.class);
        String name = "name";
        Object expected = new Object();

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttribute(name)).thenReturn(expected);
        
        Object result = this.facade.getAttribute(name);
        
        assertSame(expected, result);
    }
    
    @Test
    public void setAttribute() {
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        
        String name = "name";
        Object value = new Object();
        Object expected = new Object();

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.setAttribute(name, value)).thenReturn(expected);
        when(this.manager.getSessionListeners()).thenReturn(listeners);
        
        Object result = this.facade.setAttribute(name, value);
        
        assertSame(expected, result);
        
        verify(listener, never()).attributeAdded(this.facade, name, value);
        verify(listener).attributeUpdated(this.facade, name, value, expected);
        verify(listener, never()).attributeRemoved(same(this.facade), same(name), any());
    }
    
    @Test
    public void setNewAttribute() {
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String name = "name";
        Object value = new Object();
        Object expected = null;

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.setAttribute(name, value)).thenReturn(expected);
        when(this.manager.getSessionListeners()).thenReturn(listeners);
        
        Object result = this.facade.setAttribute(name, value);
        
        assertSame(expected, result);
        
        verify(listener).attributeAdded(this.facade, name, value);
        verify(listener, never()).attributeUpdated(same(this.facade), same(name), same(value), any());
        verify(listener, never()).attributeRemoved(same(this.facade), same(name), any());
    }
    
    @Test
    public void setNullAttribute() {
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
        
        Object result = this.facade.setAttribute(name, value);
        
        assertSame(expected, result);
        
        verify(listener, never()).attributeAdded(this.facade, name, value);
        verify(listener, never()).attributeUpdated(same(this.facade), same(name), same(value), any());
        verify(listener).attributeRemoved(this.facade, name, expected);
    }
    
    @Test
    public void setSameAttribute() {
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String name = "name";
        Object value = new Object();
        Object expected = value;

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.setAttribute(name, value)).thenReturn(expected);
        when(this.manager.getSessionListeners()).thenReturn(listeners);
        
        Object result = this.facade.setAttribute(name, value);
        
        assertSame(expected, result);
        
        verify(listener, never()).attributeAdded(this.facade, name, value);
        verify(listener, never()).attributeUpdated(same(this.facade), same(name), same(value), any());
        verify(listener, never()).attributeRemoved(same(this.facade), same(name), any());
    }
    
    @Test
    public void removeAttribute() {
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String name = "name";
        Object expected = new Object();

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.removeAttribute(name)).thenReturn(expected);
        when(this.manager.getSessionListeners()).thenReturn(listeners);
        
        Object result = this.facade.removeAttribute(name);
        
        assertSame(expected, result);
        
        verify(listener).attributeRemoved(this.facade, name, expected);
    }
    
    @Test
    public void removeNonExistingAttribute() {
        SessionAttributes attributes = mock(SessionAttributes.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String name = "name";

        when(this.session.getAttributes()).thenReturn(attributes);
        when(attributes.removeAttribute(name)).thenReturn(null);
        when(this.manager.getSessionListeners()).thenReturn(listeners);
        
        Object result = this.facade.removeAttribute(name);
        
        assertNull(result);
        
        verify(listener, never()).attributeRemoved(same(this.facade), same(name), any());
    }
    
    @Test
    public void invalidate() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionManager<Void> manager = mock(SessionManager.class);
        Batcher batcher = mock(Batcher.class);
        SessionListener listener = mock(SessionListener.class);
        SessionListeners listeners = new SessionListeners();
        listeners.addSessionListener(listener);
        String sessionId = "session";
        
        when(this.manager.getSessionListeners()).thenReturn(listeners);
        when(this.session.getId()).thenReturn(sessionId);
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.getBatcher()).thenReturn(batcher);
        
        this.facade.invalidate(exchange);
        
        verify(this.session).invalidate();
        verify(this.config).clearSession(exchange, sessionId);
        verify(listener).sessionDestroyed(this.facade, exchange, SessionDestroyedReason.INVALIDATED);
        verify(batcher).endBatch(true);
    }
    
    @Test
    public void getSessionManager() {
        assertSame(this.manager, this.facade.getSessionManager());
    }
    
    @Test
    public void changeSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
        SessionManager<Void> manager = mock(SessionManager.class);
        Session<Void> session = mock(Session.class);
        SessionAttributes oldAttributes = mock(SessionAttributes.class);
        SessionAttributes newAttributes = mock(SessionAttributes.class);
        SessionMetaData oldMetaData = mock(SessionMetaData.class);
        SessionMetaData newMetaData = mock(SessionMetaData.class);
        String sessionId = "session";
        String route = "route";
        String routedSessionid = "session:route";
        String name = "name";
        Object value = new Object();
        Date date = new Date();
        long interval = 10L;
        ArgumentCaptor<TimeUnit> capturedUnit = ArgumentCaptor.forClass(TimeUnit.class);
        
        when(this.manager.getSessionManager()).thenReturn(manager);
        when(manager.createSessionId()).thenReturn(sessionId);
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
        when(manager.locate(sessionId)).thenReturn(route);
        when(this.manager.format(sessionId, route)).thenReturn(routedSessionid);
        
        String result = this.facade.changeSessionId(exchange, config);
        
        assertSame(sessionId, result);
        
        verify(newMetaData).setLastAccessedTime(date);
        verify(newMetaData).setMaxInactiveInterval(interval, capturedUnit.getValue());
        verify(config).setSessionId(exchange, routedSessionid);
    }
}
