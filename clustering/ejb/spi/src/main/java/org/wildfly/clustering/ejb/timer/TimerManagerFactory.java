/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import org.wildfly.clustering.ee.Batch;

/**
 * Factory for creating a {@link TimerManager}.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 * @param <B> the batch type
 */
public interface TimerManagerFactory<I, B extends Batch> {

    TimerManager<I, B> createTimerManager(TimerManagerConfiguration<I, B> configuration);
}
