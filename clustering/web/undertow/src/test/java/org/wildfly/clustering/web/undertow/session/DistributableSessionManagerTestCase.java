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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.undertow.UndertowOptions;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpServerConnection;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.util.Protocols;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionMetaData;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;
import org.xnio.channels.Configurable;
import org.xnio.conduits.ConduitStreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;
import org.xnio.conduits.StreamSinkConduit;
import org.xnio.conduits.StreamSourceConduit;

public class DistributableSessionManagerTestCase {
    private final String deploymentName = "mydeployment.war";
    private final SessionManager<Map<String, Object>, Batch> manager = mock(SessionManager.class);
    private final SessionListener listener = mock(SessionListener.class);
    private final SessionListeners listeners = new SessionListeners();
    private final RecordableSessionManagerStatistics statistics = mock(RecordableSessionManagerStatistics.class);

    private DistributableSessionManager adapter;

    @Before
    public void init() {
        DistributableSessionManagerConfiguration config = mock(DistributableSessionManagerConfiguration.class);

        when(config.getDeploymentName()).thenReturn(this.deploymentName);
        when(config.getSessionListeners()).thenReturn(this.listeners);
        when(config.getSessionManager()).thenReturn(this.manager);
        when(config.getStatistics()).thenReturn(this.statistics);
        when(config.isOrphanSessionAllowed()).thenReturn(false);

        this.adapter = new DistributableSessionManager(config);
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
        when(this.manager.getStopTimeout()).thenReturn(Duration.ZERO);

        this.adapter.stop();

        verify(this.manager).stop();
    }

    @Test
    public void setDefaultSessionTimeout() {
        this.adapter.setDefaultSessionTimeout(10);

        verify(this.manager).setDefaultMaxInactiveInterval(Duration.ofSeconds(10L));
    }

    @Test
    public void createSessionResponseCommitted() {
        // Ugh - all this, just to get HttpServerExchange.isResponseStarted() to return true
        Configurable configurable = mock(Configurable.class);
        StreamSourceConduit sourceConduit = mock(StreamSourceConduit.class);
        ConduitStreamSourceChannel sourceChannel = new ConduitStreamSourceChannel(configurable, sourceConduit);
        StreamSinkConduit sinkConduit = mock(StreamSinkConduit.class);
        ConduitStreamSinkChannel sinkChannel = new ConduitStreamSinkChannel(configurable, sinkConduit);
        StreamConnection stream = mock(StreamConnection.class);

        when(stream.getSourceChannel()).thenReturn(sourceChannel);
        when(stream.getSinkChannel()).thenReturn(sinkChannel);

        ByteBufferPool bufferPool = mock(ByteBufferPool.class);
        HttpHandler handler = mock(HttpHandler.class);
        HttpServerConnection connection = new HttpServerConnection(stream, bufferPool, handler, OptionMap.create(UndertowOptions.ALWAYS_SET_DATE, false), 0, null);
        HttpServerExchange exchange = new HttpServerExchange(connection);
        exchange.setProtocol(Protocols.HTTP_1_1);
        exchange.getResponseChannel();

        SessionConfig config = mock(SessionConfig.class);

        Assert.assertThrows(IllegalStateException.class, () -> this.adapter.createSession(exchange, config));
    }

    @Test
    public void createSessionNoSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        SessionConfig config = mock(SessionConfig.class);
        Session<Map<String, Object>> session = mock(Session.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        String sessionId = "session";

        when(this.manager.createIdentifier()).thenReturn(sessionId);
        when(this.manager.createSession(sessionId)).thenReturn(session);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(session.getId()).thenReturn(sessionId);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.isNew()).thenReturn(true);

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
        Session<Map<String, Object>> session = mock(Session.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        String sessionId = "session";

        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.createSession(sessionId)).thenReturn(session);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(session.getId()).thenReturn(sessionId);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.isNew()).thenReturn(true);

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
        Session<Map<String, Object>> session = mock(Session.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        String sessionId = "session";

        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.findSession(sessionId)).thenReturn(session);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(session.getId()).thenReturn(sessionId);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session sessionAdapter = this.adapter.getSession(exchange, config);

        assertNotNull(sessionAdapter);

        verifyNoInteractions(this.statistics);

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
        Batch batch1 = mock(Batch.class);
        Batch batch2 = mock(Batch.class);
        ImmutableSession session = mock(ImmutableSession.class);
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        String id = "ABC123";

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch1, batch2);
        when(this.manager.readSession(id)).thenReturn(session);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.isNew()).thenReturn(false);

        io.undertow.server.session.Session result = this.adapter.getSession(id);

        Assert.assertSame(id, result.getId());

        verify(batch1).close();
        verify(batch2).close();
    }

    @Test
    public void getSessionByIdentifierNotExists() {
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        String id = "ABC123";

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.readSession(id)).thenReturn(null);

        io.undertow.server.session.Session result = this.adapter.getSession(id);

        Assert.assertNull(result);

        verify(batch).close();
        reset(batch);
    }

    @Test
    public void getStatistics() {
        assertSame(this.statistics, this.adapter.getStatistics());
    }
}
