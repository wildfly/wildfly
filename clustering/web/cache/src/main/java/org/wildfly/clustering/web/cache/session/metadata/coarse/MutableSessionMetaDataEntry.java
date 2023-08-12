/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.ee.cache.offset.Value;

/**
 * {@link SessionMetaDataEntry} decorator that captures mutations via a {@link MutableSessionMetaDataOffsetValues}.
 * @author Paul Ferraro
 */
public class MutableSessionMetaDataEntry implements SessionMetaDataEntry {
    private final ImmutableSessionMetaDataEntry entry;
    private final Value<Duration> timeout;
    private final Value<Instant> lastAccessStartTime;
    private final Value<Instant> lastAccessEndTime;

    public MutableSessionMetaDataEntry(ImmutableSessionMetaDataEntry entry, MutableSessionMetaDataOffsetValues values) {
        this.entry = entry;
        this.timeout = values.getTimeout();
        this.lastAccessStartTime = values.getLastAccessStartTime();
        this.lastAccessEndTime = values.getLastAccessEndTime();
    }

    @Override
    public boolean isNew() {
        return this.entry.isNew();
    }

    @Override
    public Instant getCreationTime() {
        return this.entry.getCreationTime();
    }

    @Override
    public Duration getTimeout() {
        return this.timeout.get();
    }

    @Override
    public void setTimeout(Duration timeout) {
        this.timeout.set(timeout);
    }

    @Override
    public Value<Instant> getLastAccessStartTime() {
        return this.lastAccessStartTime;
    }

    @Override
    public Value<Instant> getLastAccessEndTime() {
        return this.lastAccessEndTime;
    }
}
