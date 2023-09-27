/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.expiration;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.ee.expiration.ExpirationMetaData;

/**
 * Simple {@link ExpirationMetaData} implementation.
 * @author Paul Ferraro
 */
public class SimpleExpirationMetaData implements ExpirationMetaData {

    private final Duration timeout;
    private final Instant lastAccessTime;

    public SimpleExpirationMetaData(ExpirationMetaData metaData) {
        this(metaData.getTimeout(), metaData.getLastAccessTime());
    }

    SimpleExpirationMetaData(Duration timeout, Instant lastAccessedTime) {
        this.timeout = timeout;
        this.lastAccessTime = lastAccessedTime;
    }

    @Override
    public Duration getTimeout() {
        return this.timeout;
    }

    @Override
    public Instant getLastAccessTime() {
        return this.lastAccessTime;
    }
}
