/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.wildfly.clustering.ee.cache.offset.OffsetValue;
import org.wildfly.common.function.Functions;

/**
 * Default contextual session metadata entry.
 * @author Paul Ferraro
 */
public class DefaultSessionMetaDataEntry<C> implements ContextualSessionMetaDataEntry<C> {

    private volatile Duration timeout = Duration.ZERO;
    // The start time of the last access, expressed as an offset from the creation time
    private final OffsetValue<Instant> lastAccessStartTime;
    // The end time of the last access, expressed an an offset from the start time of the last access
    private final OffsetValue<Instant> lastAccessEndTime;
    private final AtomicReference<C> context = new AtomicReference<>();

    public DefaultSessionMetaDataEntry() {
        // Only retain millisecond precision
        this(Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    public DefaultSessionMetaDataEntry(Instant creationTime) {
        this.lastAccessStartTime = OffsetValue.from(creationTime);
        this.lastAccessEndTime = this.lastAccessStartTime.rebase();
    }

    @Override
    public boolean isNew() {
        return this.getLastAccessEndTime().getOffset().isZero();
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
    public OffsetValue<Instant> getLastAccessStartTime() {
        return this.lastAccessStartTime;
    }

    @Override
    public OffsetValue<Instant> getLastAccessEndTime() {
        return this.lastAccessEndTime;
    }

    @Override
    public C getContext(Supplier<C> factory) {
        return this.context.updateAndGet(context -> Optional.ofNullable(context).orElseGet(factory));
    }

    @Override
    public ContextualSessionMetaDataEntry<C> remap(SessionMetaDataEntryOffsets delta) {
        ContextualSessionMetaDataEntry<C> result = new DefaultSessionMetaDataEntry<>(this.getCreationTime());
        result.setTimeout(delta.getTimeoutOffset().apply(this.timeout));
        result.getLastAccessStartTime().set(delta.getLastAccessStartTimeOffset().apply(this.lastAccessStartTime.get()));
        result.getLastAccessEndTime().set(delta.getLastAccessEndTimeOffset().apply(this.lastAccessEndTime.get()));
        result.getContext(Functions.constantSupplier(this.context.get()));
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName()).append(" { ");
        builder.append("created = ").append(this.lastAccessStartTime.getBasis());
        builder.append(", timeout = ").append(this.timeout);
        builder.append(", last-access-start = ").append(this.lastAccessStartTime.get());
        builder.append(", last-access-end = ").append(this.lastAccessEndTime.get());
        return builder.append(" }").toString();
    }
}
