/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

/**
 * Factory for creating a {@link TimerManager}.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public interface TimerManagerFactory<I> {

    TimerManager<I> createTimerManager(TimerManagerConfiguration<I> configuration);
}
