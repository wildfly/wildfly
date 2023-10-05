/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session.metadata;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * An immutable "snapshot" of a session's meta-data which can be accessed outside the scope of a transaction.
 * @author Paul Ferraro
 */
public class SimpleImmutableSessionMetaData implements ImmutableSessionMetaData {

    private final boolean newSession;
    private final Instant creationTime;
    private final Instant lastAccessStartTime;
    private final Instant lastAccessEndTime;
    private final Duration timeout;

    public SimpleImmutableSessionMetaData(ImmutableSessionMetaData metaData) {
        this.newSession = metaData.isNew();
        this.creationTime = metaData.getCreationTime();
        this.lastAccessStartTime = metaData.getLastAccessStartTime();
        this.lastAccessEndTime = metaData.getLastAccessEndTime();
        this.timeout = metaData.getTimeout();
    }

    @Override
    public boolean isNew() {
        return this.newSession;
    }

    @Override
    public Instant getCreationTime() {
        return this.creationTime;
    }

    @Override
    public Instant getLastAccessStartTime() {
        return this.lastAccessStartTime;
    }

    @Override
    public Instant getLastAccessEndTime() {
        return this.lastAccessEndTime;
    }

    @Override
    public Duration getTimeout() {
        return this.timeout;
    }
}
