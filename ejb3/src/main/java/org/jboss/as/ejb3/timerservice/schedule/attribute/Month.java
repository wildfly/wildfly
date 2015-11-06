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
package org.jboss.as.ejb3.timerservice.schedule.attribute;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Month
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class Month extends IntegerBasedExpression {

    public static final Integer MAX_MONTH = 12;

    public static final Integer MIN_MONTH = 1;

    private static final Map<String, Integer> MONTH_ALIAS = new HashMap<String, Integer>();

    static {
        MONTH_ALIAS.put("jan", 1);
        MONTH_ALIAS.put("feb", 2);
        MONTH_ALIAS.put("mar", 3);
        MONTH_ALIAS.put("apr", 4);
        MONTH_ALIAS.put("may", 5);
        MONTH_ALIAS.put("jun", 6);
        MONTH_ALIAS.put("jul", 7);
        MONTH_ALIAS.put("aug", 8);
        MONTH_ALIAS.put("sep", 9);
        MONTH_ALIAS.put("oct", 10);
        MONTH_ALIAS.put("nov", 11);
        MONTH_ALIAS.put("dec", 12);

    }

    private static final int OFFSET = MONTH_ALIAS.get("jan") - Calendar.JANUARY;

    private SortedSet<Integer> offsetAdjustedMonths = new TreeSet<Integer>();

    public Month(String value) {
        super(value);
        if (OFFSET != 0) {
            for (Integer month : this.absoluteValues) {
                this.offsetAdjustedMonths.add(month - OFFSET);
            }
        } else {
            this.offsetAdjustedMonths = this.absoluteValues;
        }
    }

    @Override
    protected Integer getMaxValue() {
        return MAX_MONTH;
    }

    @Override
    protected Integer getMinValue() {
        return MIN_MONTH;
    }

    @Override
    public boolean isRelativeValue(String value) {
        // month doesn't support relative values, so always return false
        return false;
    }

    @Override
    protected boolean accepts(ScheduleExpressionType scheduleExprType) {
        switch (scheduleExprType) {
            case RANGE:
            case LIST:
            case SINGLE_VALUE:
            case WILDCARD:
                return true;
            // month doesn't support increment
            case INCREMENT:
            default:
                return false;
        }
    }

    @Override
    protected Integer parseInt(String alias) {
        try {
            return super.parseInt(alias);
        } catch (NumberFormatException nfe) {
            if (MONTH_ALIAS != null) {
                String lowerCaseAlias = alias.toLowerCase(Locale.ENGLISH);
                return MONTH_ALIAS.get(lowerCaseAlias);
            }
        }
        return null;
    }

    public Integer getNextMatch(Calendar currentCal) {
        if (this.scheduleExpressionType == ScheduleExpressionType.WILDCARD) {
            return currentCal.get(Calendar.MONTH);
        }
        if (this.offsetAdjustedMonths.isEmpty()) {
            return null;
        }
        int currentMonth = currentCal.get(Calendar.MONTH);
        for (Integer month : this.offsetAdjustedMonths) {
            if (currentMonth == month) {
                return currentMonth;
            }
            if (month > currentMonth) {
                return month;
            }
        }
        return this.offsetAdjustedMonths.first();
    }

    public Integer getFirstMatch() {
        if (this.scheduleExpressionType == ScheduleExpressionType.WILDCARD) {
            return Calendar.JANUARY;
        }
        if (this.offsetAdjustedMonths.isEmpty()) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidExpressionSeconds(this.origValue);
        }
        return this.offsetAdjustedMonths.first();
    }
}
