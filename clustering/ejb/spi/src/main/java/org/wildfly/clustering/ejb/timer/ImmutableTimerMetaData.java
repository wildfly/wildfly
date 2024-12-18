/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Describes the immutable metadata of a timer.
 * @author Paul Ferraro
 */
public interface ImmutableTimerMetaData extends TimeoutMetaData {

    /**
     * Returns the type of this timer
     * @return the timer type
     */
    TimerType getType();

    /**
     * Returns the context with which this timer was created.
     * @return the timer context
     */
    Object getContext();

    /**
     * Returns the timeout matcher, used to locate the timeout method within the associated component.
     * @return an timeout matcher
     */
    Predicate<Method> getTimeoutMatcher();

    /**
     * Indicates whether or not this timer is persistent.
     * @return true, if this is a persistent timer, false otherwise
     */
    boolean isPersistent();

    /**
     * The configuration of this timer
     * @param <C> the timer configuration type
     * @param configurationClass the configuration class
     * @return the timer configuration
     */
    <C extends TimerConfiguration> C getConfiguration(Class<C> configurationClass);

    /**
     * Returns the time of the most recent timeout event of this timer, or null if there are no previous timeout events
     * @return the optional time of the last timeout event
     */
    Optional<Instant> getLastTimeout();
}
