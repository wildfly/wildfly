/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
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
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionMetaData;
import org.wildfly.clustering.session.SessionStatistics;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;
import org.xnio.channels.Configurable;
import org.xnio.conduits.ConduitStreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;
import org.xnio.conduits.StreamSinkConduit;
import org.xnio.conduits.StreamSourceConduit;

public class DistributableSessionManagerTestCase {
    private final String deploymentName = "mydeployment.war";
    private final SessionManager<Map<String, Object>> manager = mock(SessionManager.class);
    private final SessionListener listener = mock(SessionListener.class);
    private final SessionListeners listeners = new SessionListeners();
    private final SessionStatistics stats = mock(SessionStatistics.class);
    private final RecordableSessionManagerStatistics statistics = mock(RecordableSessionManagerStatistics.class);
    private final Supplier<String> identifierFactory = mock(Supplier.class);
    private final Supplier<Batch> batchFactory = mock(Supplier.class);

    private DistributableSessionManager adapter;

    @Before
    public void init() {
        when(this.manager.getStatistics()).thenReturn(this.stats);
        when(this.manager.getBatchFactory()).thenReturn(this.batchFactory);
        when(this.manager.getIdentifierFactory()).thenReturn(this.identifierFactory);

        DistributableSessionManagerConfiguration config = mock(DistributableSessionManagerConfiguration.class);

        when(config.getDeploymentName()).thenReturn(this.deploymentName);
        when(config.getSessionListeners()).thenReturn(this.listeners);
        when(config.getSessionManager()).thenReturn(this.manager);
        when(config.getStatistics()).thenReturn(this.statistics);

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
        this.adapter.stop();

        verify(this.manager).stop();
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
        String id = "foo";
        int expectedTimeout = 10;

        this.adapter.setDefaultSessionTimeout(expectedTimeout);

        when(this.identifierFactory.get()).thenReturn(id);
        when(stream.getSourceChannel()).thenReturn(sourceChannel);
        when(stream.getSinkChannel()).thenReturn(sinkChannel);

        ByteBufferPool bufferPool = mock(ByteBufferPool.class);
        HttpHandler handler = mock(HttpHandler.class);
        HttpServerConnection connection = new HttpServerConnection(stream, bufferPool, handler, OptionMap.create(UndertowOptions.ALWAYS_SET_DATE, false), 0, null);
        HttpServerExchange exchange = new HttpServerExchange(connection);
        exchange.setProtocol(Protocols.HTTP_1_1);
        exchange.getResponseChannel();

        SessionConfig config = mock(SessionConfig.class);

        io.undertow.server.session.Session session = this.adapter.createSession(exchange, config);

        // Verify that a nonce session was created
        verify(this.manager, never()).createSession(id);

        Assert.assertEquals(id, session.getId());
        Assert.assertEquals(expectedTimeout, session.getMaxInactiveInterval());
    }

    @Test
    public void createSessionNoSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batch batch = mock(Batch.class);
        SuspendedBatch suspendedBatch = mock(SuspendedBatch.class);
        Context<Batch> context = mock(Context.class);
        SessionConfig config = mock(SessionConfig.class);
        Session<Map<String, Object>> session = mock(Session.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        String sessionId = "session";

        when(this.batchFactory.get()).thenReturn(batch);
        when(batch.suspend()).thenReturn(suspendedBatch);
        when(suspendedBatch.resume()).thenReturn(batch);
        when(suspendedBatch.resumeWithContext()).thenReturn(context);
        when(context.get()).thenReturn(batch);
        when(this.identifierFactory.get()).thenReturn(sessionId);
        when(this.manager.createSession(sessionId)).thenReturn(session);
        when(session.getId()).thenReturn(sessionId);
        when(session.getMetaData()).thenReturn(metaData);

        io.undertow.server.session.Session sessionAdapter = this.adapter.createSession(exchange, config);

        assertNotNull(sessionAdapter);

        verify(this.listener).sessionCreated(sessionAdapter, exchange);
        verify(config).setSessionId(exchange, sessionId);
        verify(batch).suspend();
        verify(this.statistics).record(metaData);

        String expected = "expected";
        when(session.getId()).thenReturn(expected);

        String result = sessionAdapter.getId();
        assertSame(expected, result);
    }

    @Test
    public void createSessionSpecifiedSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batch batch = mock(Batch.class);
        SuspendedBatch suspendedBatch = mock(SuspendedBatch.class);
        Context<Batch> context = mock(Context.class);
        SessionConfig config = mock(SessionConfig.class);
        Session<Map<String, Object>> session = mock(Session.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        String sessionId = "session";

        when(this.batchFactory.get()).thenReturn(batch);
        when(batch.suspend()).thenReturn(suspendedBatch);
        when(suspendedBatch.resume()).thenReturn(batch);
        when(suspendedBatch.resumeWithContext()).thenReturn(context);
        when(context.get()).thenReturn(batch);
        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.createSession(sessionId)).thenReturn(session);
        when(session.getId()).thenReturn(sessionId);
        when(session.getMetaData()).thenReturn(metaData);

        io.undertow.server.session.Session sessionAdapter = this.adapter.createSession(exchange, config);

        assertNotNull(sessionAdapter);

        verify(this.listener).sessionCreated(sessionAdapter, exchange);
        verify(batch).suspend();
        verify(this.statistics).record(metaData);
        verifyNoInteractions(this.identifierFactory);

        String expected = "expected";
        when(session.getId()).thenReturn(expected);

        String result = sessionAdapter.getId();
        assertSame(expected, result);
    }

    @Test
    public void createSessionAlreadyExists() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batch batch = mock(Batch.class);
        SuspendedBatch suspendedBatch = mock(SuspendedBatch.class);
        Context<Batch> context = mock(Context.class);
        SessionConfig config = mock(SessionConfig.class);
        String sessionId = "session";

        when(this.batchFactory.get()).thenReturn(batch);
        when(batch.suspend()).thenReturn(suspendedBatch);
        when(suspendedBatch.resume()).thenReturn(batch);
        when(suspendedBatch.resumeWithContext()).thenReturn(context);
        when(context.get()).thenReturn(batch);
        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.createSession(sessionId)).thenReturn(null);

        IllegalStateException exception = null;
        try {
            this.adapter.createSession(exchange, config);
        } catch (IllegalStateException e) {
            exception = e;
        }

        assertNotNull(exception);

        verifyNoInteractions(this.identifierFactory);
        verify(batch).discard();
        verify(batch).close();
    }

    @Test
    public void getSession() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batch batch = mock(Batch.class);
        SuspendedBatch suspendedBatch = mock(SuspendedBatch.class);
        Context<Batch> context = mock(Context.class);
        SessionConfig config = mock(SessionConfig.class);
        Session<Map<String, Object>> session = mock(Session.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        String sessionId = "session";

        when(this.batchFactory.get()).thenReturn(batch);
        when(batch.suspend()).thenReturn(suspendedBatch);
        when(suspendedBatch.resume()).thenReturn(batch);
        when(suspendedBatch.resumeWithContext()).thenReturn(context);
        when(context.get()).thenReturn(batch);
        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.findSession(sessionId)).thenReturn(session);
        when(session.getId()).thenReturn(sessionId);
        when(session.isValid()).thenReturn(true);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.isExpired()).thenReturn(false);
        when(metaData.getLastAccessTime()).thenReturn(Optional.of(Instant.now()));

        io.undertow.server.session.Session sessionAdapter = this.adapter.getSession(exchange, config);

        assertNotNull(sessionAdapter);

        verifyNoInteractions(this.statistics);
        verifyNoInteractions(this.identifierFactory);

        verify(batch).suspend();

        String expected = "expected";
        when(session.getId()).thenReturn(expected);

        String result = sessionAdapter.getId();
        assertSame(expected, result);
    }

    @Test
    public void getSessionNoSessionId() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
        Batch batch = mock(Batch.class);
        SuspendedBatch suspendedBatch = mock(SuspendedBatch.class);
        Context<Batch> context = mock(Context.class);

        when(this.batchFactory.get()).thenReturn(batch);
        when(batch.suspend()).thenReturn(suspendedBatch);
        when(suspendedBatch.resume()).thenReturn(batch);
        when(suspendedBatch.resumeWithContext()).thenReturn(context);
        when(context.get()).thenReturn(batch);
        when(config.findSessionId(exchange)).thenReturn(null);

        io.undertow.server.session.Session sessionAdapter = this.adapter.getSession(exchange, config);

        assertNull(sessionAdapter);

        verify(batch, never()).discard();
        verify(batch).close();
    }

    @Test
    public void getSessionInvalidCharacters() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        SessionConfig config = mock(SessionConfig.class);
        Batch batch = mock(Batch.class);
        SuspendedBatch suspendedBatch = mock(SuspendedBatch.class);
        Context<Batch> context = mock(Context.class);
        String sessionId = "session+";

        when(this.batchFactory.get()).thenReturn(batch);
        when(batch.suspend()).thenReturn(suspendedBatch);
        when(suspendedBatch.resume()).thenReturn(batch);
        when(suspendedBatch.resumeWithContext()).thenReturn(context);
        when(context.get()).thenReturn(batch);
        when(config.findSessionId(exchange)).thenReturn(sessionId);

        io.undertow.server.session.Session sessionAdapter = this.adapter.getSession(exchange, config);

        assertNull(sessionAdapter);

        verify(this.manager, never()).findSession(sessionId);
        verify(batch, never()).discard();
        verify(batch).close();

        sessionAdapter = this.adapter.getSession(sessionId);

        assertNull(sessionAdapter);

        verify(this.manager, never()).findSession(sessionId);
        verify(batch, never()).discard();
        verify(batch, times(2)).close();
    }

    @Test
    public void getSessionNotExists() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batch batch = mock(Batch.class);
        SuspendedBatch suspendedBatch = mock(SuspendedBatch.class);
        Context<Batch> context = mock(Context.class);
        SessionConfig config = mock(SessionConfig.class);
        String sessionId = "session";

        when(this.batchFactory.get()).thenReturn(batch);
        when(batch.suspend()).thenReturn(suspendedBatch);
        when(suspendedBatch.resume()).thenReturn(batch);
        when(suspendedBatch.resumeWithContext()).thenReturn(context);
        when(context.get()).thenReturn(batch);
        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.findSession(sessionId)).thenReturn(null);
        when(this.manager.getBatchFactory()).thenReturn(Supplier.of(batch));

        io.undertow.server.session.Session sessionAdapter = this.adapter.getSession(exchange, config);

        assertNull(sessionAdapter);

        verify(batch, never()).discard();
        verify(batch).close();
    }

    @Test
    public void getSessionInvalid() {
        HttpServerExchange exchange = new HttpServerExchange(null);
        Batch batch = mock(Batch.class);
        SuspendedBatch suspendedBatch = mock(SuspendedBatch.class);
        Context<Batch> context = mock(Context.class);
        SessionConfig config = mock(SessionConfig.class);
        Session<Map<String, Object>> session = mock(Session.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        String sessionId = "session";

        when(this.batchFactory.get()).thenReturn(batch);
        when(batch.suspend()).thenReturn(suspendedBatch);
        when(suspendedBatch.resumeWithContext()).thenReturn(context);
        when(context.get()).thenReturn(batch);
        when(config.findSessionId(exchange)).thenReturn(sessionId);
        when(this.manager.findSession(sessionId)).thenReturn(session);
        when(session.getId()).thenReturn(sessionId);
        when(session.isValid()).thenReturn(false);
        when(session.getMetaData()).thenReturn(metaData);
        when(metaData.isExpired()).thenReturn(false);

        io.undertow.server.session.Session sessionAdapter = this.adapter.getSession(exchange, config);

        assertNull(sessionAdapter);

        verifyNoInteractions(this.statistics);

        verify(batch, never()).discard();
        verify(batch).close();
    }

    @Test
    public void activeSessions() {
        when(this.stats.getActiveSessions()).thenReturn(Collections.singleton("expected"));

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
        when(this.stats.getActiveSessions()).thenReturn(Collections.singleton(expected));

        Set<String> result = this.adapter.getActiveSessions();

        assertEquals(1, result.size());
        assertSame(expected, result.iterator().next());
    }

    @Test
    public void getAllSessions() {
        String expected = "expected";
        when(this.stats.getSessions()).thenReturn(Collections.singleton(expected));

        Set<String> result = this.adapter.getAllSessions();

        assertEquals(1, result.size());
        assertSame(expected, result.iterator().next());
    }

    @Test
    public void getSessionByIdentifier() {
        Session<Map<String, Object>> session = mock(Session.class);
        SessionMetaData metaData = mock(SessionMetaData.class);
        String id = "ABC123";

        doReturn(session).when(this.manager).getDetachedSession(id);
        doReturn(id).when(session).getId();
        doReturn(metaData).when(session).getMetaData();
        doReturn(false).when(session).isValid();

        io.undertow.server.session.Session result = this.adapter.getSession(id);

        Assert.assertNull(result);

        doReturn(true).when(session).isValid();

        result = this.adapter.getSession(id);

        Assert.assertSame(id, result.getId());
    }

    @Test
    public void getStatistics() {
        assertSame(this.statistics, this.adapter.getStatistics());
    }
}
