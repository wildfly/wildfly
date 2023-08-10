/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session;

import java.time.Duration;
import java.time.Instant;

/**
 * Composite view of the meta data of a session, combining volatile and static aspects.
 * @author Paul Ferraro
 */
public class CompositeSessionMetaData implements InvalidatableSessionMetaData {

    private final SessionCreationMetaData creationMetaData;
    private final SessionAccessMetaData accessMetaData;

    public CompositeSessionMetaData(SessionCreationMetaData creationMetaData, SessionAccessMetaData accessMetaData) {
        this.creationMetaData = creationMetaData;
        this.accessMetaData = accessMetaData;
    }

    @Override
    public boolean isNew() {
        return this.creationMetaData.isNew();
    }

    @Override
    public boolean isValid() {
        return this.creationMetaData.isValid();
    }

    @Override
    public boolean invalidate() {
        return this.creationMetaData.invalidate();
    }

    @Override
    public Instant getCreationTime() {
        return this.creationMetaData.getCreationTime();
    }

    @Override
    public Instant getLastAccessStartTime() {
        return this.getCreationTime().plus(this.accessMetaData.getSinceCreationDuration());
    }

    @Override
    public Instant getLastAccessTime() {
        return this.getLastAccessStartTime().plus(this.accessMetaData.getLastAccessDuration());
    }

    @Override
    public Duration getTimeout() {
        return this.creationMetaData.getTimeout();
    }

    @Override
    public void setLastAccess(Instant startTime, Instant endTime) {
        Instant creationTime = this.creationMetaData.getCreationTime();
        this.accessMetaData.setLastAccessDuration(!startTime.equals(creationTime) ? Duration.between(creationTime, startTime) : Duration.ZERO, Duration.between(startTime, endTime));
    }

    @Override
    public void setTimeout(Duration duration) {
        this.creationMetaData.setTimeout(duration.isNegative() ? Duration.ZERO : duration);
    }

    @Override
    public void close() {
        this.creationMetaData.close();
    }
}
