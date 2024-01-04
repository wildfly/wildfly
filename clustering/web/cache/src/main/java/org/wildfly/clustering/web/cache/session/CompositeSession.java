/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session;

import java.util.function.Supplier;

import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.cache.Contextual;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributes;
import org.wildfly.clustering.web.cache.session.metadata.InvalidatableSessionMetaData;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Generic session implementation - independent of cache mapping strategy.
 * @author Paul Ferraro
 */
public class CompositeSession<L> extends CompositeImmutableSession implements Session<L> {

    private final InvalidatableSessionMetaData metaData;
    private final SessionAttributes attributes;
    private final Contextual<L> contextual;
    private final Supplier<L> contextFactory;
    private final Remover<String> remover;

    public CompositeSession(String id, InvalidatableSessionMetaData metaData, SessionAttributes attributes, Contextual<L> contextual, Supplier<L> contextFactory, Remover<String> remover) {
        super(id, metaData, attributes);
        this.metaData = metaData;
        this.attributes = attributes;
        this.contextual = contextual;
        this.contextFactory = contextFactory;
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
        return this.contextual.getContext(this.contextFactory);
    }
}
