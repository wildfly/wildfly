/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Default immutable session metadata implementation that delegates to a cache entry.
 * @author Paul Ferraro
 */
public class DefaultImmutableSessionMetaData implements ImmutableSessionMetaData {

    private final ImmutableSessionMetaDataEntry entry;

    public DefaultImmutableSessionMetaData(ImmutableSessionMetaDataEntry entry) {
        this.entry = entry;
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
    public Instant getLastAccessStartTime() {
        return this.entry.getLastAccessStartTime().get();
    }

    @Override
    public Instant getLastAccessEndTime() {
        return this.entry.getLastAccessEndTime().get();
    }

    @Override
    public Duration getTimeout() {
        return this.entry.getTimeout();
    }
}
