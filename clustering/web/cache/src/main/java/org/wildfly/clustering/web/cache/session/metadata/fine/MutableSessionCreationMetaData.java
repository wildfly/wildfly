/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.ee.cache.offset.Value;

/**
 * @author Paul Ferraro
 */
public class MutableSessionCreationMetaData implements SessionCreationMetaData {

    private final ImmutableSessionCreationMetaData metaData;
    private final Value<Duration> timeout;

    public MutableSessionCreationMetaData(ImmutableSessionCreationMetaData metaData, Value<Duration> timeout) {
        this.metaData = metaData;
        this.timeout = timeout;
    }

    @Override
    public Instant getCreationTime() {
        return this.metaData.getCreationTime();
    }

    @Override
    public Duration getTimeout() {
        return this.timeout.get();
    }

    @Override
    public void setTimeout(Duration timeout) {
        this.timeout.set(timeout);
    }
}
