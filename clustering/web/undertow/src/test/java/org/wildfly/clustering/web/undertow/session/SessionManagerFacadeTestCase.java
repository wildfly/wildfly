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
import static org.mockito.Mockito.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;

public class SessionManagerFacadeTestCase {
    @SuppressWarnings("unchecked")
    private final SessionManager<Void> manager = mock(SessionManager.class);
    private final ReplicationConfig config = new ReplicationConfig();
    private final SessionListener listener = mock(SessionListener.class);

    private SessionManagerFacade facade = new SessionManagerFacade(this.manager, this.config);

    @Before
    public void init() {
        this.facade.registerSessionListener(this.listener);
    }

    @Test
    public void parse() {
        Map.Entry<String, String> result = this.facade.parse("session1.route1");
        assertEquals("session1", result.getKey());
        assertEquals("route1", result.getValue());

        result = this.facade.parse("session2");
        assertEquals("session2", result.getKey());
        assertNull(result.getValue());

        result = this.facade.parse(null);
        assertNull(result.getKey());
        assertNull(result.getValue());
    }

    @Test
    public void format() {
        assertEquals("session1.route1", this.facade.format("session1", "route1"));
        assertEquals("session2", this.facade.format("session2", null));
        assertNull(this.facade.format(null, null));
    }

    @Test
    public void start() {
        this.facade.start();
        
        verify(this.manager).start();
    }

    @Test
    public void stop() {
        this.facade.stop();
        
        verify(this.manager).stop();
    }

    @Test
    public void setDefaultSessionTimeout() {
        this.facade.setDefaultSessionTimeout(10);
        
        verify(this.manager).setDefaultMaxInactiveInterval(10L, TimeUnit.SECONDS);
    }

    @Test
    public void createSessionNoSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher batcher = mock(Batcher.class);
        SessionConfig config = mock(SessionConfig.class);
        @SuppressWarnings("unchecked")
        Session<Void> session = mock(Session.class);
        String sessionId = "session";
        String route = "route";
        String routingSessionId = "session.route";
        
        when(this.manager.createSessionId()).thenReturn(sessionId);
        when(this.manager.containsSession(sessionId)).thenReturn(false);
        when(this.manager.createSession(sessionId)).thenReturn(session);
        when(this.manager.locate(sessionId)).thenReturn(route);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.startBatch()).thenReturn(true);
        when(session.getId()).thenReturn(sessionId);
        
        io.undertow.server.session.Session sessionFacade = this.facade.createSession(exchange, config);
        
        assertNotNull(sessionFacade);
        
        verify(this.listener).sessionCreated(sessionFacade, exchange);
        verify(config).setSessionId(exchange, routingSessionId);
        
        String expected = "expected";
        when(session.getId()).thenReturn(expected);
        
        String result = sessionFacade.getId();
        assertSame(expected, result);
    }

    @Test
    public void createSessionSpecifiedSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher batcher = mock(Batcher.class);
        SessionConfig config = mock(SessionConfig.class);
        @SuppressWarnings("unchecked")
        Session<Void> session = mock(Session.class);
        String requestedSessionId = "session.route1";
        String sessionId = "session";
        String route = "route";
        String routingSessionId = "session.route";
        
        when(config.findSessionId(exchange)).thenReturn(requestedSessionId);
        when(this.manager.containsSession(sessionId)).thenReturn(false);
        when(this.manager.createSession(sessionId)).thenReturn(session);
        when(this.manager.locate(sessionId)).thenReturn(route);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.startBatch()).thenReturn(true);
        when(session.getId()).thenReturn(sessionId);
        
        io.undertow.server.session.Session sessionFacade = this.facade.createSession(exchange, config);
        
        assertNotNull(sessionFacade);
        
        verify(this.listener).sessionCreated(sessionFacade, exchange);
        verify(config).setSessionId(exchange, routingSessionId);
        
        String expected = "expected";
        when(session.getId()).thenReturn(expected);
        
        String result = sessionFacade.getId();
        assertSame(expected, result);
    }

    @Test
    public void createSessionAlreadyExists() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
        String requestedSessionId = "session.route1";
        String sessionId = "session";
        
        when(config.findSessionId(exchange)).thenReturn(requestedSessionId);
        when(this.manager.containsSession(sessionId)).thenReturn(true);
        
        IllegalStateException exception = null;
        try {
            this.facade.createSession(exchange, config);
        } catch (IllegalStateException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    @Test
    public void getSession() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher batcher = mock(Batcher.class);
        SessionConfig config = mock(SessionConfig.class);
        @SuppressWarnings("unchecked")
        Session<Void> session = mock(Session.class);
        String requestedSessionId = "session.route1";
        String sessionId = "session";
        String route = "route2";
        String routingSessionId = "session.route2";
        
        when(config.findSessionId(exchange)).thenReturn(requestedSessionId);
        when(this.manager.findSession(sessionId)).thenReturn(session);
        when(this.manager.locate(sessionId)).thenReturn(route);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.startBatch()).thenReturn(true);
        when(session.getId()).thenReturn(sessionId);

        io.undertow.server.session.Session sessionFacade = this.facade.getSession(exchange, config);
        
        assertNotNull(sessionFacade);
        
        verify(config).setSessionId(exchange, routingSessionId);
        
        String expected = "expected";
        when(session.getId()).thenReturn(expected);
        
        String result = sessionFacade.getId();
        assertSame(expected, result);
    }

    @Test
    public void getSessionNoSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
        
        when(config.findSessionId(exchange)).thenReturn(null);

        io.undertow.server.session.Session sessionFacade = this.facade.getSession(exchange, config);
        
        assertNull(sessionFacade);
    }

    @Test
    public void getSessionNotExists() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher batcher = mock(Batcher.class);
        SessionConfig config = mock(SessionConfig.class);
        String requestedSessionId = "session.route1";
        String sessionId = "session";
        
        when(config.findSessionId(exchange)).thenReturn(requestedSessionId);
        when(this.manager.findSession(sessionId)).thenReturn(null);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.startBatch()).thenReturn(true);

        io.undertow.server.session.Session sessionFacade = this.facade.getSession(exchange, config);
        
        assertNull(sessionFacade);
        
        verify(batcher).endBatch(false);
    }
}
