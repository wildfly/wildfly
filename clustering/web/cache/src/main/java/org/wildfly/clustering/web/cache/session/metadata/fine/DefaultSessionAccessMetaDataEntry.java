/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.time.Duration;

/**
 * @author Paul Ferraro
 */
public class DefaultSessionAccessMetaDataEntry implements SessionAccessMetaDataEntry {

    private volatile Duration sinceCreation = Duration.ZERO;
    private volatile Duration lastAccess = Duration.ZERO;

    @Override
    public boolean isNew() {
        return this.getLastAccessDuration().isZero();
    }

    @Override
    public Duration getSinceCreationDuration() {
        return this.sinceCreation;
    }

    @Override
    public Duration getLastAccessDuration() {
        return this.lastAccess;
    }

    @Override
    public void setLastAccessDuration(Duration sinceCreation, Duration lastAccess) {
        this.sinceCreation = sinceCreation;
        this.lastAccess = lastAccess;
    }

    @Override
    public SessionAccessMetaDataEntry remap(SessionAccessMetaDataEntryOffsets delta) {
        SessionAccessMetaDataEntry result = new DefaultSessionAccessMetaDataEntry();
        result.setLastAccessDuration(delta.getSinceCreationOffset().apply(this.sinceCreation), delta.getLastAccessOffset().apply(this.lastAccess));
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName()).append(" { ");
        builder.append("since-creation = ").append(this.sinceCreation);
        builder.append(", last-access = ").append(this.lastAccess);
        return builder.append("}").toString();
    }
}
