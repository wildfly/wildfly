/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.time.Instant;
import java.util.function.UnaryOperator;

/**
 * Creates an operator for a given schedule expression, used to determine the next time a scheduled timer should timeout.
 * @author Paul Ferraro
 */
public interface ScheduleTimerOperationProvider {

    UnaryOperator<Instant> createOperator(ImmutableScheduleExpression expression);
}
