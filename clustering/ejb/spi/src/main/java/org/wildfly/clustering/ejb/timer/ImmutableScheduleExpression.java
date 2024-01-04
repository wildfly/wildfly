/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Immutable view of a {@link jakarta.ejb.ScheduleExpression}.
 * @author Paul Ferraro
 */
public interface ImmutableScheduleExpression {
    String getSecond();
    String getMinute();
    String getHour();

    String getDayOfMonth();
    String getMonth();
    String getDayOfWeek();
    String getYear();

    ZoneId getZone();

    Instant getStart();
    Instant getEnd();
}
