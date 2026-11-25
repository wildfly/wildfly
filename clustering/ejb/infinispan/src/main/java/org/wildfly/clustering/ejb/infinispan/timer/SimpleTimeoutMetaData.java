/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Instant;
import java.util.Optional;

import org.wildfly.clustering.ejb.timer.TimeoutMetaData;

/**
 * @author Paul Ferraro
 */
public class SimpleTimeoutMetaData implements TimeoutMetaData {

    private final Optional<Instant> nextTimeout;

    SimpleTimeoutMetaData(TimeoutMetaData metaData) {
        this(metaData.getNextTimeout());
    }

    SimpleTimeoutMetaData(Optional<Instant> nextTimeout) {
        this.nextTimeout = nextTimeout;
    }

    @Override
    public Optional<Instant> getNextTimeout() {
        return this.nextTimeout;
    }
}
