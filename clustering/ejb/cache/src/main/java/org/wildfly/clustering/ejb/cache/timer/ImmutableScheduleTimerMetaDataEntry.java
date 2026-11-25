/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerType;

/**
 * An immutable view of a schedule-based timer metadata cache entry.
 * @author Paul Ferraro
 * @param <C> the timer context type
 */
public interface ImmutableScheduleTimerMetaDataEntry<C> extends ImmutableTimerMetaDataEntry<C>, ScheduleTimerConfiguration {

    @Override
    default TimerType getType() {
        return TimerType.SCHEDULE;
    }
}
