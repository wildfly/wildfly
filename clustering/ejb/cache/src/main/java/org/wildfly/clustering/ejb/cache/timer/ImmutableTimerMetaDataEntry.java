/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.wildfly.clustering.ejb.timer.TimerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerType;

/**
 * @author Paul Ferraro
 * @param <C> the timer context type
 */
public interface ImmutableTimerMetaDataEntry<C> extends TimerConfiguration, UnaryOperator<Instant> {
    /**
     * Returns the type of this timer
     * @return the timer type
     */
    TimerType getType();

    /**
     * Returns the context with which this timer was created.
     * @return the timer context
     */
    C getContext();

    /**
     * Returns the timeout matcher, used to locate the timeout method within the associated component.
     * @return an timeout matcher
     */
    default Predicate<Method> getTimeoutMatcher() {
        return DefaultTimeoutMatcher.INSTANCE;
    }

    /**
     * Supplies the time of the most recent timeout event of this timer, or null if there are no previous timeout events
     * @return a reference to the time of the last timeout event, if one exists
     */
    Duration getLastTimeout();
}
