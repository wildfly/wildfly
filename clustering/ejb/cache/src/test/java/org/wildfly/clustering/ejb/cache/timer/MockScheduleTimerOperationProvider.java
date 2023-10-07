/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.ejb.timer.ScheduleTimerOperationProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(ScheduleTimerOperationProvider.class)
public class MockScheduleTimerOperationProvider implements ScheduleTimerOperationProvider {

    @Override
    public UnaryOperator<Instant> createOperator(ImmutableScheduleExpression expression) {
        return instant -> Optional.ofNullable(instant).map(last -> last.plus(Duration.ofSeconds(1))).orElse(Optional.ofNullable(expression.getStart()).orElseGet(Instant::now));
    }
}
