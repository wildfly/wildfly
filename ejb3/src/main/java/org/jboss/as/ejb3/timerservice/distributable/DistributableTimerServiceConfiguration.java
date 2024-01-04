/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice.distributable;

import java.util.function.Function;

import org.jboss.as.ejb3.timerservice.spi.ManagedTimerServiceConfiguration;

/**
 * Encapsulates the configuration of a {@link DistributedTimerService}.
 * @author Paul Ferraro
 * @param <I> the timer identifier type
 */
public interface DistributableTimerServiceConfiguration<I> extends ManagedTimerServiceConfiguration {

    Function<String, I> getIdentifierParser();
    TimerSynchronizationFactory<I> getTimerSynchronizationFactory();
}
