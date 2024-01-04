/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.time.Instant;

/**
 * Describes the metadata of a timer.
 * @author Paul Ferraro
 */
public interface TimerMetaData extends ImmutableTimerMetaData {

    /**
     * Updates the time of the last timeout event for this timer
     * @param timeout the time of the timeout event
     */
    void setLastTimeout(Instant timeout);
}
