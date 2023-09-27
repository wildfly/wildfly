/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Paul Ferraro
 */
public class SimpleSessionCreationMetaData implements SessionCreationMetaData {

    private final Instant creationTime;
    private volatile Duration timeout = Duration.ZERO;
    private volatile boolean newSession;
    private final AtomicBoolean valid = new AtomicBoolean(true);

    public SimpleSessionCreationMetaData() {
        // Only retain millisecond precision
        this.creationTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        this.newSession = true;
    }

    public SimpleSessionCreationMetaData(Instant creationTime) {
        this.creationTime = creationTime;
        this.newSession = false;
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
    public Duration getTimeout() {
        return this.timeout;
    }

    @Override
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean isValid() {
        return this.valid.get();
    }

    @Override
    public boolean invalidate() {
        return this.valid.compareAndSet(true, false);
    }

    @Override
    public void close() {
        this.newSession = false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName()).append(" { ");
        builder.append("created = ").append(this.creationTime);
        builder.append(", timeout = ").append(this.timeout);
        return builder.append(" }").toString();
    }
}
