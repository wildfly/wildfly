/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.ejb.timer.IntervalTimerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerType;

/**
 * @author Paul Ferraro
 */
public interface IntervalTimerCreationMetaData<V> extends TimerCreationMetaData<V>, IntervalTimerConfiguration {
    @Override
    default TimerType getType() {
        return TimerType.INTERVAL;
    }
}
