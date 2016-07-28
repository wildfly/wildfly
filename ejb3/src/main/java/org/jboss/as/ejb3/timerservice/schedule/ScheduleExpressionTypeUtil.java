/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.timerservice.schedule;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.schedule.value.RangeValue;
import org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType;

/**
 * Utility for {@link javax.ejb.ScheduleExpression}
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class ScheduleExpressionTypeUtil {
    /**
     * Returns the corresponding {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType} for the passed value
     *
     * @param value The value to be parsed
     * @return
     */
    public static ScheduleExpressionType getType(String value) {
        if (value == null) {
            throw EjbLogger.EJB3_TIMER_LOGGER.valueIsNull();
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
