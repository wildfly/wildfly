/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ejb.timer.TimerMetaData;

/**
 * A timer metadata implementation that triggers a mutator on {@link #setLastTimeout(Instant)}.
 * @author Paul Ferraro
 */
public class DefaultTimerMetaData<C> extends DefaultImmutableTimerMetaData<C> implements TimerMetaData {

    private final TimerMetaDataEntry<C> entry;
    private final Mutator mutator;

    public DefaultTimerMetaData(TimerMetaDataConfiguration<C> configuration, TimerMetaDataEntry<C> entry, Mutator mutator) {
        super(configuration, entry);
        this.entry = entry;
        this.mutator = mutator;
    }

    @Override
    public void setLastTimeout(Instant timeout) {
        this.entry.setLastTimeout((timeout != null) ? Duration.between(this.entry.getStart(), timeout) : null);
        this.mutator.mutate();
    }
}
