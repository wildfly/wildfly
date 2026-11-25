/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

/**
 * Encapsulates the configuration of a {@link TimerManager}.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public interface TimerManagerConfiguration<I> extends TimerManagerFactoryConfiguration<I> {

    TimeoutListener<I> getListener();
}
