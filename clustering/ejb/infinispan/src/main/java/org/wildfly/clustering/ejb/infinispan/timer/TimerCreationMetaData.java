/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.wildfly.clustering.ejb.timer.TimerConfiguration;
import org.wildfly.clustering.ejb.timer.TimerType;

/**
 * @author Paul Ferraro
 */
public interface TimerCreationMetaData<V> extends TimerConfiguration, UnaryOperator<Instant> {
    final Predicate<Method> DEFAULT_TIMEOUT_MATCHER = method -> false;

    TimerType getType();
    V getContext();

    default Predicate<Method> getTimeoutMatcher() {
        return DEFAULT_TIMEOUT_MATCHER;
    }
}
