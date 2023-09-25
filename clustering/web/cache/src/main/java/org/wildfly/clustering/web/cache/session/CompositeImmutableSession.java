/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session;

import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Generic immutable session implementation - independent of cache mapping strategy.
 * @author Paul Ferraro
 */
public class CompositeImmutableSession implements ImmutableSession {

    private final String id;
    private final ImmutableSessionMetaData metaData;
    private final ImmutableSessionAttributes attributes;

    public CompositeImmutableSession(String id, ImmutableSessionMetaData metaData, ImmutableSessionAttributes attributes) {
        this.id = id;
        this.metaData = metaData;
        this.attributes = attributes;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public ImmutableSessionAttributes getAttributes() {
        return this.attributes;
    }

    @Override
    public ImmutableSessionMetaData getMetaData() {
        return this.metaData;
    }
}
