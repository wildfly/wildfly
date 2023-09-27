/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.ee.Mutator;

/**
 * @author Paul Ferraro
 */
public class MutableSessionCreationMetaData implements SessionCreationMetaData {

    private final SessionCreationMetaData metaData;
    private final Mutator mutator;

    public MutableSessionCreationMetaData(SessionCreationMetaData metaData, Mutator mutator) {
        this.metaData = metaData;
        this.mutator = mutator;
    }

    @Override
    public boolean isNew() {
        return this.metaData.isNew();
    }

    @Override
    public Instant getCreationTime() {
        return this.metaData.getCreationTime();
    }

    @Override
    public Duration getTimeout() {
        return this.metaData.getTimeout();
    }

    @Override
    public void setTimeout(Duration duration) {
        if (!this.metaData.getTimeout().equals(duration)) {
            this.metaData.setTimeout(duration);
            this.mutator.mutate();
        }
    }

    @Override
    public boolean isValid() {
        return this.metaData.isValid();
    }

    @Override
    public boolean invalidate() {
        return this.metaData.invalidate();
    }

    @Override
    public void close() {
        this.metaData.close();
    }
}
