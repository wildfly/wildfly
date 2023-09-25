/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.timerservice.schedule.value;

import org.jboss.as.ejb3.logging.EjbLogger;

/**
 * Represents the type of expression used in the values of a {@link jakarta.ejb.ScheduleExpression}
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public enum ScheduleExpressionType {
    /**
     * Represents a single value type.
     * <p>
     * For example:
     * <ul>
     * <li>second = "10"</li>
     * <li>month = "Jun"</li>
     * </ul>
     * <p/>
     * <p/>
     * </p>
     */
    SINGLE_VALUE,

    /**
     * Represents a wildcard "*" value.
     * <p/>
     * <p>
     * For example:
     * <ul>
     * <li>second = "*"</li>
     * </ul>
     * </p>
     */
    WILDCARD,

    /**
     * Represents a value represented as a list.
     * <p/>
     * <p>
     * For example:
     * <ul>
     * <li>second = "1, 10"</li>
     * <li>dayOfMonth = "Sun, Fri, Mon"</li>
     * <ul>
     * </p>
     */
    LIST,

    /**
     * Represents a value represented as a range.
     * <p>
     * For example:
     * <ul>
     * <li>minute = "0-15"</li>
     * <li>year = "2009-2012</li>
     * </ul>
     * </p>
     */
    RANGE,

    /**
     * Represents a value represented as an increment.
     * <p>
     * For example:
     * <ul>
     * <li>hour = "* &#47; 3"</li>
     * <li>minute = "20 &#47; 10</li>
     * </ul>
     * </p>
     */
    INCREMENT;

    public static ScheduleExpressionType getType(String value) {
        if (value == null) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidScheduleValue("", value);
        }
        // Order of check is important.
        // TODO: Explain why this order is important

        if (value.trim().equals("*")) {
            return ScheduleExpressionType.WILDCARD;
        }
        if (value.contains(",")) {
            return ScheduleExpressionType.LIST;
        }
        if (value.contains("-") && RangeValue.accepts(value)) {
            return ScheduleExpressionType.RANGE;
        }
        if (value.contains("/")) {
            return ScheduleExpressionType.INCREMENT;
        }
        return ScheduleExpressionType.SINGLE_VALUE;
    }

}
