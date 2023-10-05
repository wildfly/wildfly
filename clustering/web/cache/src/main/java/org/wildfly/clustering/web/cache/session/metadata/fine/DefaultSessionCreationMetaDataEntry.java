/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.wildfly.clustering.ee.cache.offset.Offset;
import org.wildfly.common.function.Functions;

/**
 * @author Paul Ferraro
 */
public class DefaultSessionCreationMetaDataEntry<C> implements SessionCreationMetaDataEntry<C> {

    private final Instant creationTime;
    private volatile Duration timeout = Duration.ZERO;
    private final AtomicReference<C> context = new AtomicReference<>();

    public DefaultSessionCreationMetaDataEntry() {
        // Only retain millisecond precision
        this(Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    public DefaultSessionCreationMetaDataEntry(Instant creationTime) {
        this.creationTime = creationTime;
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
    public C getContext(Supplier<C> factory) {
        return this.context.updateAndGet(context -> Optional.ofNullable(context).orElseGet(factory));
    }

    @Override
    public SessionCreationMetaDataEntry<C> remap(Supplier<Offset<Duration>> timeoutOffset) {
        SessionCreationMetaDataEntry<C> result = new DefaultSessionCreationMetaDataEntry<>(this.creationTime);
        result.setTimeout(timeoutOffset.get().apply(this.timeout));
        result.getContext(Functions.constantSupplier(this.context.get()));
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName()).append(" { ");
        builder.append("created = ").append(this.creationTime);
        builder.append(", timeout = ").append(this.timeout);
        return builder.append(" }").toString();
    }
}
