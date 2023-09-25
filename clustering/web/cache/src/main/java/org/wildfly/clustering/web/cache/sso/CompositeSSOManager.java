/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.sso;

import java.util.Map;
import java.util.function.Supplier;

import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.IdentifierFactory;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.Sessions;

public class CompositeSSOManager<AV, SV, A, D, S, L> implements SSOManager<A, D, S, L, TransactionBatch> {

    private final SSOFactory<AV, SV, A, D, S, L> factory;
    private final Batcher<TransactionBatch> batcher;
    private final IdentifierFactory<String> identifierFactory;

    public CompositeSSOManager(SSOFactory<AV, SV, A, D, S, L> factory, IdentifierFactory<String> identifierFactory, Batcher<TransactionBatch> batcher) {
        this.factory = factory;
        this.batcher = batcher;
        this.identifierFactory = identifierFactory;
    }

    @Override
    public SSO<A, D, S, L> createSSO(String ssoId, A authentication) {
        Map.Entry<AV, SV> value = this.factory.createValue(ssoId, authentication);
        return this.factory.createSSO(ssoId, value);
    }

    @Override
    public SSO<A, D, S, L> findSSO(String ssoId) {
        Map.Entry<AV, SV> value = this.factory.findValue(ssoId);
        return (value != null) ? this.factory.createSSO(ssoId, value) : null;
    }

    @Override
    public Sessions<D, S> findSessionsContaining(S session) {
        SessionsFactory<SV, D, S> factory = this.factory.getSessionsFactory();
        Map.Entry<String, SV> entry = factory.findEntryContaining(session);
        return (entry != null) ? factory.createSessions(entry.getKey(), entry.getValue()) : null;
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
    public void start() {
        this.identifierFactory.start();
    }

    @Override
    public void stop() {
        this.identifierFactory.stop();
    }
}
