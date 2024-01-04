/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

/**
 * A {@link Timer} that executes according to a configured schedule.
 * @author Paul Ferraro
 */
public interface ScheduledTimer<I> extends Timer<I> {

    @Override
    TimerMetaData getMetaData();
}
