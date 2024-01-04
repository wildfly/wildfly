/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.time.Instant;

/**
 * Encapsulates the configuration of a schedule timer.
 * @author Paul Ferraro
 */
public interface ScheduleTimerConfiguration extends TimerConfiguration {

    ImmutableScheduleExpression getScheduleExpression();

    @Override
    default Instant getStart() {
        return this.getScheduleExpression().getStart();
    }
}
