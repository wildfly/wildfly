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
package org.jboss.as.ejb3.timerservice.schedule.value;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jboss.as.ejb3.logging.EjbLogger;

/**
 * Represents a value for a {@link javax.ejb.ScheduleExpression} which is expressed as a list type. A
 * {@link ListValue} comprises of values separated by a ",".
 * <p/>
 * <p>
 * Each value in the {@link ListValue} must be an individual attribute value or a range.
 * List items <b>cannot</b> themselves be lists, wild-cards, or increments.
 * Duplicate values are allowed, but are ignored.
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 * @see ScheduleExpressionType#LIST
 */
public class ListValue implements ScheduleValue {

    /**
     * Separator used for parsing a {@link String} which represents
     * a {@link ListValue}
     */
    public static final String LIST_SEPARATOR = ",";

    /**
     * The individual values in a {@link ListValue}
     * <p>
     * Each value in this set may be a {@link String} representing a {@link SingleValue}
     * or a {@link RangeValue}
     * </p>
     */
    private final List<String> values = new ArrayList<String>();

    /**
     * Creates a {@link ListValue} by parsing the passed <code>value</code>.
     *
     * @param list The value to be parsed
     * @throws IllegalArgumentException If the passed <code>value</code> cannot be
     *                                  represented as an {@link ListValue}
     */
    public ListValue(String list) {
        if (list == null || list.isEmpty()) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidListExpression(list);
        }
        StringTokenizer tokenizer = new StringTokenizer(list, LIST_SEPARATOR);
        while (tokenizer.hasMoreTokens()) {
            String value = tokenizer.nextToken().trim();
            this.values.add(value);
        }
        // a list MUST minimally contain 2 elements
        // Ex: "," "1," ", 2" are all invalid
        if (this.values.size() < 2) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidListExpression(list);
        }
    }

    /**
     * Returns the values that make up the {@link ListValue}.
     * <p>
     * Each value in this set may be a {@link String} representing a {@link SingleValue}
     * or a {@link RangeValue}
     * </p>
     *
     * @return
     */
    public List<String> getValues() {
        return this.values;
    }

}
