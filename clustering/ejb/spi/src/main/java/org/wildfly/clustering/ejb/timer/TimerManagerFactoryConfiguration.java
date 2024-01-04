/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.util.function.Supplier;

/**
 * Encapsulates the configuration of a {@link TimerManagerFactory}.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public interface TimerManagerFactoryConfiguration<I> {

    TimerServiceConfiguration getTimerServiceConfiguration();
    Supplier<I> getIdentifierFactory();
    TimerRegistry<I> getRegistry();
    boolean isPersistent();
}
