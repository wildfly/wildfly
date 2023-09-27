/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import org.wildfly.clustering.ee.Batch;

/**
 * Encapsulates the configuration of a {@link TimerManager}.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 * @param <B> the batch type
 */
public interface TimerManagerConfiguration<I, B extends Batch> extends TimerManagerFactoryConfiguration<I> {

    TimeoutListener<I, B> getListener();
}
