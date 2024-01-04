/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session;

import org.wildfly.clustering.web.cache.session.attributes.SimpleImmutableSessionAttributes;
import org.wildfly.clustering.web.cache.session.metadata.SimpleImmutableSessionMetaData;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * An immutable "snapshot" of a session which can be accessed outside the scope of a transaction.
 * @author Paul Ferraro
 */
public class SimpleImmutableSession implements ImmutableSession {

    private final String id;
    private final boolean valid;
    private final ImmutableSessionMetaData metaData;
    private final ImmutableSessionAttributes attributes;

    public SimpleImmutableSession(ImmutableSession session) {
        this.id = session.getId();
        this.valid = session.isValid();
        this.metaData = new SimpleImmutableSessionMetaData(session.getMetaData());
        this.attributes = new SimpleImmutableSessionAttributes(session.getAttributes());
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }

    @Override
    public ImmutableSessionMetaData getMetaData() {
        return this.metaData;
    }

    @Override
    public ImmutableSessionAttributes getAttributes() {
        return this.attributes;
    }
}
