/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerType;

/**
 * @author Paul Ferraro
 */
public interface ScheduleTimerCreationMetaData<V> extends TimerCreationMetaData<V>, ScheduleTimerConfiguration {

    @Override
    default TimerType getType() {
        return TimerType.SCHEDULE;
    }

    @Override
    TimeoutDescriptor getTimeoutMatcher();
}
