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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;

import org.junit.Before;
import org.junit.Test;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;

public class DistributableSessionManagerTestCase {
    private final String deploymentName = "mydeployment.war";
    private final SessionManager<LocalSessionContext, Batch> manager = mock(SessionManager.class);
    private final SessionListener listener = mock(SessionListener.class);
    private final SessionListeners listeners = new SessionListeners();
    private final RecordableSessionManagerStatistics statistics = mock(RecordableSessionManagerStatistics.class);

    private final DistributableSessionManager adapter = new DistributableSessionManager(this.deploymentName, this.manager, this.listeners, this.statistics);

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
        verify(this.statistics).reset();
    }

    @Test
    public void stop() {
        when(this.manager.getDefaultMaxInactiveInterval()).thenReturn(Duration.ZERO);

        this.adapter.stop();

        verify(this.manager).stop();
    }

    @Test
    public void setDefaultSessionTimeout() {
        this.adapter.setDefaultSessionTimeout(10);

        verify(this.manager).setDefaultMaxInactiveInterval(Duration.ofSeconds(10L));
    }

    @Test
    public void createSessionNoSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        SessionConfig config = mock(SessionConfig.class);
        Session<LocalSessionContext> session = mock(Session.class);
        String sessionId = "session";

        when(this.manager.createIdentifier()).thenReturn(sessionId);
        when(this.manager.createSession(sessionId)).thenReturn(session);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(session.getId()).thenReturn(sessionId);

        io.undertow.server.session.Session sessionAdapter = this.adapter.createSession(exchange, config);

        assertNotNull(sessionAdapter);

        verify(this.listener).sessionCreated(sessionAdapter, exchange);
        verify(config).setSessionId(exchange, sessionId);
        verify(batcher).suspendBatch();
        verify(this.statistics).record(sessionAdapter);

        String expected = "expected";
        when(session.getId()).thenReturn(expected);

        String result = sessionAdapter.getId();
        assertSame(expected, result);
    }

    @Test
    public void createSessionSpecifiedSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        SessionConfig config = mock(SessionConfig.class);
        Session<LocalSessionContext> session = mock(Session.class);
        String sessionId = "session";

        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.createSession(sessionId)).thenReturn(session);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(session.getId()).thenReturn(sessionId);

        io.undertow.server.session.Session sessionAdapter = this.adapter.createSession(exchange, config);

        assertNotNull(sessionAdapter);

        verify(this.listener).sessionCreated(sessionAdapter, exchange);
        verify(batcher).suspendBatch();
        verify(this.statistics).record(sessionAdapter);

        String expected = "expected";
        when(session.getId()).thenReturn(expected);

        String result = sessionAdapter.getId();
        assertSame(expected, result);
    }

    @Test
    public void createSessionAlreadyExists() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        SessionConfig config = mock(SessionConfig.class);
        String sessionId = "session";

        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.createSession(sessionId)).thenReturn(null);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);

        IllegalStateException exception = null;
        try {
            this.adapter.createSession(exchange, config);
        } catch (IllegalStateException e) {
            exception = e;
        }

        assertNotNull(exception);

        verify(batch).discard();
        verify(batch).close();
    }

    @Test
    public void getSession() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        SessionConfig config = mock(SessionConfig.class);
        Session<LocalSessionContext> session = mock(Session.class);
        String sessionId = "session";

        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.findSession(sessionId)).thenReturn(session);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(session.getId()).thenReturn(sessionId);

        io.undertow.server.session.Session sessionAdapter = this.adapter.getSession(exchange, config);

        assertNotNull(sessionAdapter);

        verifyZeroInteractions(this.statistics);

        verify(batcher).suspendBatch();

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
    public void getSessionInvalidCharacters() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
        String sessionId = "session+";

        when(config.findSessionId(exchange)).thenReturn(sessionId);

        io.undertow.server.session.Session sessionAdapter = this.adapter.getSession(exchange, config);

        assertNull(sessionAdapter);

        sessionAdapter = this.adapter.getSession(sessionId);

        assertNull(sessionAdapter);

        verify(this.manager, never()).findSession(sessionId);
    }


    @Test
    public void getSessionNotExists() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        SessionConfig config = mock(SessionConfig.class);
        String sessionId = "session";

        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.findSession(sessionId)).thenReturn(null);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);

        io.undertow.server.session.Session sessionAdapter = this.adapter.getSession(exchange, config);

        assertNull(sessionAdapter);

        verify(batch).close();
        verify(batcher, never()).suspendBatch();
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
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionAttributes attributes = mock(ImmutableSessionAttributes.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        String id = "session";
        String name = "name";
        Object value = new Object();
        Set<String> names = Collections.singleton(name);
        Instant creationTime = Instant.now();
        Instant lastAccessedTime = Instant.now();
        Duration maxInactiveInterval = Duration.ofMinutes(30L);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(this.manager.viewSession(id)).thenReturn(session);
        when(session.getId()).thenReturn(id);
        when(session.getAttributes()).thenReturn(attributes);
        when(attributes.getAttributeNames()).thenReturn(names);
        when(attributes.getAttribute(name)).thenReturn(value);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.getCreationTime()).thenReturn(creationTime);
        when(metaData.getLastAccessedTime()).thenReturn(lastAccessedTime);
        when(metaData.getMaxInactiveInterval()).thenReturn(maxInactiveInterval);
        when(batcher.createBatch()).thenReturn(batch);

        io.undertow.server.session.Session result = this.adapter.getSession(id);

        assertSame(this.adapter, result.getSessionManager());
        assertSame(id, result.getId());
        assertEquals(creationTime.toEpochMilli(), result.getCreationTime());
        assertEquals(lastAccessedTime.toEpochMilli(), result.getLastAccessedTime());
        assertEquals(maxInactiveInterval.getSeconds(), result.getMaxInactiveInterval());
        assertEquals(names, result.getAttributeNames());
        assertSame(value, result.getAttribute(name));

        verify(batch).close();
    }

    @Test
    public void getSessionByIdentifierNotExists() {
        String id = "session";
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(this.manager.viewSession(id)).thenReturn(null);
        when(batcher.createBatch()).thenReturn(batch);

        io.undertow.server.session.Session result = this.adapter.getSession(id);

        assertNull(result);

        verify(batch).close();
    }

    @Test
    public void getStatistics() {
        assertSame(this.statistics, this.adapter.getStatistics());
    }
}
