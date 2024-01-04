/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.hotrod.session;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ee.expiration.Expiration;
import org.wildfly.clustering.web.cache.session.SessionFactory;
import org.wildfly.clustering.web.cache.session.SimpleImmutableSession;
import org.wildfly.clustering.web.cache.session.ValidSession;
import org.wildfly.clustering.web.hotrod.logging.Logger;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.common.function.Functions;

/**
 * Generic HotRod-based session manager implementation - independent of cache mapping strategy.
 * @param <SC> the ServletContext specification type
 * @param <MV> the meta-data value type
 * @param <AV> the attributes value type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
public class HotRodSessionManager<SC, MV, AV, LC> implements SessionManager<LC, TransactionBatch> {
    private final Registrar<Consumer<ImmutableSession>> expirationListenerRegistrar;
    private final Consumer<ImmutableSession> expirationListener;
    private final SessionFactory<SC, MV, AV, LC> factory;
    private final Supplier<String> identifierFactory;
    private final SC context;
    private final Batcher<TransactionBatch> batcher;
    private final Duration stopTimeout;
    private final Consumer<ImmutableSession> closeTask = Functions.discardingConsumer();
    private final Expiration expiration;

    private volatile Registration expirationListenerRegistration;

    public HotRodSessionManager(SessionFactory<SC, MV, AV, LC> factory, HotRodSessionManagerConfiguration<SC> configuration) {
        this.factory = factory;
        this.expirationListenerRegistrar = configuration.getExpirationListenerRegistrar();
        this.expirationListener = configuration.getExpirationListener();
        this.context = configuration.getServletContext();
        this.identifierFactory = configuration.getIdentifierFactory();
        this.batcher = configuration.getBatcher();
        this.stopTimeout = configuration.getStopTimeout();
        this.expiration = configuration;
    }

    @Override
    public void start() {
        this.expirationListenerRegistration = this.expirationListenerRegistrar.register(this.expirationListener);
    }

    @Override
    public void stop() {
        if (this.expirationListenerRegistration != null) {
            this.expirationListenerRegistration.close();
        }
    }

    @Override
    public Duration getStopTimeout() {
        return this.stopTimeout;
    }

    @Override
    public Batcher<TransactionBatch> getBatcher() {
        return this.batcher;
    }

    @Override
    public Supplier<String> getIdentifierFactory() {
        return this.identifierFactory;
    }

    @Override
    public Session<LC> findSession(String id) {
        Map.Entry<MV, AV> entry = this.factory.findValue(id);
        if (entry == null) {
            Logger.ROOT_LOGGER.tracef("Session %s not found", id);
            return null;
        }
        ImmutableSession session = this.factory.createImmutableSession(id, entry);
        if (session.getMetaData().isExpired()) {
            Logger.ROOT_LOGGER.tracef("Session %s was found, but has expired", id);
            this.expirationListener.accept(session);
            this.factory.remove(id);
            return null;
        }
        return new ValidSession<>(this.factory.createSession(id, entry, this.context), this.closeTask);
    }

    @Override
    public Session<LC> createSession(String id) {
        Map.Entry<MV, AV> entry = this.factory.createValue(id, this.expiration.getTimeout());
        if (entry == null) return null;
        Session<LC> session = this.factory.createSession(id, entry, this.context);
        return new ValidSession<>(session, this.closeTask);
    }

    @Override
    public ImmutableSession readSession(String id) {
        Map.Entry<MV, AV> entry = this.factory.findValue(id);
        return (entry != null) ? new SimpleImmutableSession(this.factory.createImmutableSession(id, entry)) : null;
    }

    @Override
    public Set<String> getActiveSessions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getLocalSessions() {
        return Collections.emptySet();
    }

    @Override
    public long getActiveSessionCount() {
        return this.getActiveSessions().size();
    }
}
