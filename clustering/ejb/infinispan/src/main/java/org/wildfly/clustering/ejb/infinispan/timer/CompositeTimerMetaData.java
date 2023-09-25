/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.ejb.timer.TimerMetaData;

/**
 * @author Paul Ferraro
 */
public class CompositeTimerMetaData<V> extends CompositeImmutableTimerMetaData<V> implements TimerMetaData {

    private final TimerCreationMetaData<V> creationMetaData;
    private final TimerAccessMetaData accessMetaData;

    public CompositeTimerMetaData(TimerMetaDataConfiguration<V> configuration, TimerCreationMetaData<V> creationMetaData, TimerAccessMetaData accessMetaData) {
        super(configuration, creationMetaData, accessMetaData);
        this.creationMetaData = creationMetaData;
        this.accessMetaData = accessMetaData;
    }

    @Override
    public void setLastTimout(Instant timeout) {
        this.accessMetaData.setLastTimeout((timeout != null) ? Duration.between(this.creationMetaData.getStart(), timeout) : null);
    }
}
