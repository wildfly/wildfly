/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.time.Duration;

/**
 * Encapsulates the configuration of an interval timer.
 * @author Paul Ferraro
 */
public interface IntervalTimerConfiguration extends TimerConfiguration {

    default Duration getInterval() {
        // A single action timer has no interval
        return null;
    }
}
