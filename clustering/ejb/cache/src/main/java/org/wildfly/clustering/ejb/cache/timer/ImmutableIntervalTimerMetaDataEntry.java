/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import org.wildfly.clustering.ejb.timer.IntervalTimerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerType;

/**
 * An immutable view of an interval-based timer metadata cache entry.
 * @author Paul Ferraro
 * @param <C> the timer context type
 */
public interface ImmutableIntervalTimerMetaDataEntry<C> extends ImmutableTimerMetaDataEntry<C>, IntervalTimerConfiguration {
    @Override
    default TimerType getType() {
        return TimerType.INTERVAL;
    }
}
