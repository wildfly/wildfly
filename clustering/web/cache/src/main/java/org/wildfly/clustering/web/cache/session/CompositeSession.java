/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session;

import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Generic session implementation - independent of cache mapping strategy.
 * @author Paul Ferraro
 */
public class CompositeSession<L> extends CompositeImmutableSession implements Session<L> {

    private final InvalidatableSessionMetaData metaData;
    private final SessionAttributes attributes;
    private final AtomicReference<L> localContext;
    private final LocalContextFactory<L> localContextFactory;
    private final Remover<String> remover;

    public CompositeSession(String id, InvalidatableSessionMetaData metaData, SessionAttributes attributes, AtomicReference<L> localContext, LocalContextFactory<L> localContextFactory, Remover<String> remover) {
        super(id, metaData, attributes);
        this.metaData = metaData;
        this.attributes = attributes;
        this.localContext = localContext;
        this.localContextFactory = localContextFactory;
        this.remover = remover;
    }

    @Override
    public SessionAttributes getAttributes() {
        return this.attributes;
    }

    @Override
    public boolean isValid() {
        return this.metaData.isValid();
    }

    @Override
    public void invalidate() {
        if (this.metaData.invalidate()) {
            this.remover.remove(this.getId());
        }
    }

    @Override
    public SessionMetaData getMetaData() {
        return this.metaData;
    }

    @Override
    public void close() {
        if (this.metaData.isValid()) {
            this.attributes.close();
            this.metaData.close();
        }
    }

    @Override
    public L getLocalContext() {
        if (this.localContextFactory == null) return null;
        L localContext = this.localContext.get();
        if (localContext == null) {
            localContext = this.localContextFactory.createLocalContext();
            if (!this.localContext.compareAndSet(null, localContext)) {
                return this.localContext.get();
            }
        }
        return localContext;
    }
}
