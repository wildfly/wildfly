/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Supplier;

import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;

/**
 * @author Paul Ferraro
 */
public interface ImmutableScheduleExpressionBuilder extends Supplier<ImmutableScheduleExpression> {

    ImmutableScheduleExpressionBuilder second(String second);

    ImmutableScheduleExpressionBuilder minute(String minute);

    ImmutableScheduleExpressionBuilder hour(String hour);

    ImmutableScheduleExpressionBuilder dayOfMonth(String dayOfMonth);

    ImmutableScheduleExpressionBuilder month(String month);

    ImmutableScheduleExpressionBuilder dayOfWeek(String dayOfWeek);

    ImmutableScheduleExpressionBuilder year(String year);

    ImmutableScheduleExpressionBuilder zone(ZoneId zone);

    ImmutableScheduleExpressionBuilder start(Instant start);

    ImmutableScheduleExpressionBuilder end(Instant end);
}
