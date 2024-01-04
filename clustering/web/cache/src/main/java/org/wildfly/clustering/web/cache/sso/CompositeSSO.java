/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.sso;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;

public class CompositeSSO<A, D, S, L> implements SSO<A, D, S, L> {
    private final String id;
    private final A authentication;
    private final Sessions<D, S> sessions;
    private final AtomicReference<L> localContext;
    private final Supplier<L> localContextFactory;
    private final Remover<String> remover;

    public CompositeSSO(String id, A authentication, Sessions<D, S> sessions, AtomicReference<L> localContext, Supplier<L> localContextFactory, Remover<String> remover) {
        this.id = id;
        this.authentication = authentication;
        this.sessions = sessions;
        this.localContext = localContext;
        this.localContextFactory = localContextFactory;
        this.remover = remover;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public A getAuthentication() {
        return this.authentication;
    }

    @Override
    public Sessions<D, S> getSessions() {
        return this.sessions;
    }

    @Override
    public void invalidate() {
        this.remover.remove(this.id);
    }

    @Override
    public L getLocalContext() {
        if (this.localContextFactory == null) return null;
        L localContext = this.localContext.get();
        if (localContext == null) {
            localContext = this.localContextFactory.get();
            if (!this.localContext.compareAndSet(null, localContext)) {
                return this.localContext.get();
            }
        }
        return localContext;
    }
}
