/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.time.Duration;

import org.wildfly.clustering.ee.cache.offset.Value;

/**
 * @author Paul Ferraro
 */
public class MutableSessionAccessMetaData implements SessionAccessMetaData {

    private final ImmutableSessionAccessMetaData metaData;
    private final Value<Duration> sinceCreation;
    private final Value<Duration> lastAccess;

    public MutableSessionAccessMetaData(ImmutableSessionAccessMetaData metaData, MutableSessionAccessMetaDataValues values) {
        this.metaData = metaData;
        this.sinceCreation = values.getSinceCreation();
        this.lastAccess = values.getLastAccess();
    }

    @Override
    public boolean isNew() {
        return this.metaData.isNew();
    }

    @Override
    public Duration getSinceCreationDuration() {
        return this.sinceCreation.get();
    }

    @Override
    public Duration getLastAccessDuration() {
        return this.lastAccess.get();
    }

    @Override
    public void setLastAccessDuration(Duration sinceCreation, Duration lastAccess) {
        this.sinceCreation.set(sinceCreation);
        this.lastAccess.set(lastAccess);
    }
}
