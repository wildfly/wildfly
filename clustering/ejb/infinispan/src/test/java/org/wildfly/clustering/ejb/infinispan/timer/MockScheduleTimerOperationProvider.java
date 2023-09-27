/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

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
        return instant -> Optional.ofNullable(instant).orElse(Instant.now());
    }
}
