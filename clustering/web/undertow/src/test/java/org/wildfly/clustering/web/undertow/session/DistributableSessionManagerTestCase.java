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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.wildfly.clustering.web.Batch;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;

public class DistributableSessionManagerTestCase {
    private final String deploymentName = "mydeployment.war";
    private final SessionManager<LocalSessionContext> manager = mock(SessionManager.class);
    private final SessionListener listener = mock(SessionListener.class);

    private DistributableSessionManager adapter = new DistributableSessionManager(this.deploymentName, this.manager);

    @Before
    public void init() {
        this.adapter.registerSessionListener(this.listener);
    }

    @Test
    public void getDeploymentName() {
        assertSame(this.deploymentName, this.adapter.getDeploymentName());
    }

    @Test
    public void start() {
        this.adapter.start();
        
        verify(this.manager).start();
    }

    @Test
    public void stop() {
        this.adapter.stop();
        
        verify(this.manager).stop();
    }

    @Test
    public void setDefaultSessionTimeout() {
        this.adapter.setDefaultSessionTimeout(10);
        
        verify(this.manager).setDefaultMaxInactiveInterval(10L, TimeUnit.SECONDS);
    }

    @Test
    public void createSessionNoSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        SessionConfig config = mock(SessionConfig.class);
        Session<LocalSessionContext> session = mock(Session.class);
        String sessionId = "session";
        String existingSessionId = "existing";
        
        when(this.manager.createIdentifier()).thenReturn(existingSessionId, sessionId);
        when(this.manager.containsSession(existingSessionId)).thenReturn(true);
        when(this.manager.containsSession(sessionId)).thenReturn(false);
        when(this.manager.createSession(sessionId)).thenReturn(session);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.startBatch()).thenReturn(batch);
        when(session.getId()).thenReturn(sessionId);
        
        io.undertow.server.session.Session sessionAdapter = this.adapter.createSession(exchange, config);
        
        assertNotNull(sessionAdapter);
        
        verify(this.listener).sessionCreated(sessionAdapter, exchange);
        verify(config).setSessionId(exchange, sessionId);
        verifyZeroInteractions(batch);

        String expected = "expected";
        when(session.getId()).thenReturn(expected);
        
        String result = sessionAdapter.getId();
        assertSame(expected, result);
    }

    @Test
    public void createSessionSpecifiedSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        SessionConfig config = mock(SessionConfig.class);
        Session<LocalSessionContext> session = mock(Session.class);
        String sessionId = "session";
        
        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.containsSession(sessionId)).thenReturn(false);
        when(this.manager.createSession(sessionId)).thenReturn(session);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.startBatch()).thenReturn(batch);
        when(session.getId()).thenReturn(sessionId);
        
        io.undertow.server.session.Session sessionAdapter = this.adapter.createSession(exchange, config);
        
        assertNotNull(sessionAdapter);
        
        verify(this.listener).sessionCreated(sessionAdapter, exchange);
        verifyZeroInteractions(batch);
        
        String expected = "expected";
        when(session.getId()).thenReturn(expected);
        
        String result = sessionAdapter.getId();
        assertSame(expected, result);
    }

    @Test
    public void getSession() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        SessionConfig config = mock(SessionConfig.class);
        Session<LocalSessionContext> session = mock(Session.class);
        String sessionId = "session";
        
        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.findSession(sessionId)).thenReturn(session);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.startBatch()).thenReturn(batch);
        when(session.getId()).thenReturn(sessionId);

        io.undertow.server.session.Session sessionAdapter = this.adapter.getSession(exchange, config);
        
        assertNotNull(sessionAdapter);
        
        verifyZeroInteractions(batch);
        
        String expected = "expected";
        when(session.getId()).thenReturn(expected);
        
        String result = sessionAdapter.getId();
        assertSame(expected, result);
    }

    @Test
    public void getSessionNoSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
        
        when(config.findSessionId(exchange)).thenReturn(null);

        io.undertow.server.session.Session sessionAdapter = this.adapter.getSession(exchange, config);
        
        assertNull(sessionAdapter);
    }

    @Test
    public void getSessionNotExists() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        SessionConfig config = mock(SessionConfig.class);
        String sessionId = "session";
        
        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.findSession(sessionId)).thenReturn(null);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.startBatch()).thenReturn(batch);

        io.undertow.server.session.Session sessionAdapter = this.adapter.getSession(exchange, config);
        
        assertNull(sessionAdapter);
        
        verify(batch).close();
    }

    @Test
    public void activeSessions() {
        when(this.manager.getActiveSessions()).thenReturn(Collections.singleton("expected"));
        
        int result = this.adapter.getActiveSessions().size();
        
        assertEquals(1, result);
    }

    @Test
    public void getTransientSessions() {
        Set<String> result = this.adapter.getTransientSessions();
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void getActiveSessions() {
        String expected = "expected";
        when(this.manager.getActiveSessions()).thenReturn(Collections.singleton(expected));

        Set<String> result = this.adapter.getActiveSessions();
        
        assertEquals(1, result.size());
        assertSame(expected, result.iterator().next());
    }

    @Test
    public void getAllSessions() {
        String expected = "expected";
        when(this.manager.getLocalSessions()).thenReturn(Collections.singleton(expected));

        Set<String> result = this.adapter.getAllSessions();
        
        assertEquals(1, result.size());
        assertSame(expected, result.iterator().next());
    }

    @Test
    public void getSessionByIdentifier() {
        ImmutableSession session = mock(ImmutableSession.class);
        String id = "session";
        Batcher batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(this.manager.viewSession(id)).thenReturn(session);
        when(session.getId()).thenReturn(id);
        when(batcher.startBatch()).thenReturn(batch);
        
        io.undertow.server.session.Session result = this.adapter.getSession(id);
        
        assertSame(this.adapter, result.getSessionManager());
        assertSame(id, result.getId());
        
        verify(batch).discard();
    }

    @Test
    public void getSessionByIdentifierNotExists() {
        String id = "session";
        Batcher batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(this.manager.viewSession(id)).thenReturn(null);
        when(batcher.startBatch()).thenReturn(batch);
        
        io.undertow.server.session.Session result = this.adapter.getSession(id);
        
        assertNull(result);

        verify(batch).discard();
    }
}
