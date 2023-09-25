/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.time.Duration;

import org.wildfly.clustering.ee.Mutator;

/**
 * @author Paul Ferraro
 */
public class MutableSessionAccessMetaData implements SessionAccessMetaData {

    private final SessionAccessMetaData metaData;
    private final Mutator mutator;

    public MutableSessionAccessMetaData(SessionAccessMetaData metaData, Mutator mutator) {
        this.metaData = metaData;
        this.mutator = mutator;
    }

    @Override
    public Duration getSinceCreationDuration() {
        return this.metaData.getSinceCreationDuration();
    }

    @Override
    public Duration getLastAccessDuration() {
        return this.metaData.getLastAccessDuration();
    }

    @Override
    public void setLastAccessDuration(Duration sinceCreation, Duration lastAccess) {
        this.metaData.setLastAccessDuration(sinceCreation, lastAccess);
        this.mutator.mutate();
    }
}
