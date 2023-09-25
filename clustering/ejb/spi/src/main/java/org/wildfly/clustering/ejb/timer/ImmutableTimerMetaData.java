/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.function.Predicate;

/**
 * Describes the immutable metadata of a timer.
 * @author Paul Ferraro
 */
public interface ImmutableTimerMetaData {

    TimerType getType();
    Object getContext();
    Predicate<Method> getTimeoutMatcher();
    boolean isPersistent();
    <C extends TimerConfiguration> C getConfiguration(Class<C> configurationClass);

    Instant getNextTimeout();
    Instant getLastTimout();
}
