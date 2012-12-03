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
package org.jboss.as.ejb3.timerservice.persistence;

import java.lang.reflect.Method;
import java.util.Date;

import javax.ejb.ScheduleExpression;

import org.jboss.as.ejb3.timerservice.CalendarTimer;
import org.jboss.as.ejb3.timerservice.schedule.CalendarBasedTimeout;

/**
 * DO NOT MODIFY THIS CLASS
 *
 * Due to a temporary implementation that became permanent, the {@link org.jboss.as.ejb3.timerservice.persistence.filestore.FileTimerPersistence}
 * writes these out directly, modifying this class will break compatibility
 *
 * @author Jaikiran Pai
 * @author Stuart Douglas
 */
public class CalendarTimerEntity extends TimerEntity {

    private transient ScheduleExpression scheduleExpression;

    private transient CalendarBasedTimeout calendarTimeout;

    private final String scheduleExprSecond;

    private final String scheduleExprMinute;

    private final String scheduleExprHour;

    private final String scheduleExprDayOfWeek;

    private final String scheduleExprDayOfMonth;

    private final String scheduleExprMonth;

    private final String scheduleExprYear;

    private final Date scheduleExprStartDate;

    private final Date scheduleExprEndDate;

    private final String scheduleExprTimezone;

    private final boolean autoTimer;

    private final TimeoutMethod timeoutMethod;

    public CalendarTimerEntity(CalendarTimer calendarTimer) {
        super(calendarTimer);
        this.scheduleExpression = calendarTimer.getScheduleExpression();
        this.autoTimer = calendarTimer.isAutoTimer();
        if (calendarTimer.isAutoTimer()) {
            Method method = calendarTimer.getTimeoutMethod();
            Class<?>[] methodParams = method.getParameterTypes();
            String[] params = new String[methodParams.length];
            for (int i = 0; i < methodParams.length; i++) {
                params[i] = methodParams[i].getName();
            }
            this.timeoutMethod = new TimeoutMethod(method.getDeclaringClass().getName(), method.getName(), params);
        } else {
            this.timeoutMethod = null;
        }

        this.scheduleExprSecond = this.scheduleExpression.getSecond();
        this.scheduleExprMinute = this.scheduleExpression.getMinute();
        this.scheduleExprHour = this.scheduleExpression.getHour();
        this.scheduleExprDayOfMonth = this.scheduleExpression.getDayOfMonth();
        this.scheduleExprMonth = this.scheduleExpression.getMonth();
        this.scheduleExprDayOfWeek = this.scheduleExpression.getDayOfWeek();
        this.scheduleExprYear = this.scheduleExpression.getYear();
        this.scheduleExprStartDate = this.scheduleExpression.getStart();
        this.scheduleExprEndDate = this.scheduleExpression.getEnd();
        this.scheduleExprTimezone = this.scheduleExpression.getTimezone();

    }

    @Override
    public boolean isCalendarTimer() {
        return true;
    }

    public ScheduleExpression getScheduleExpression() {
        if (this.scheduleExpression == null) {
            this.scheduleExpression = new ScheduleExpression();
            this.scheduleExpression.second(this.scheduleExprSecond).minute(this.scheduleExprMinute).hour(this.scheduleExprHour).dayOfWeek(this.scheduleExprDayOfWeek)
                    .dayOfMonth(this.scheduleExprDayOfMonth).month(this.scheduleExprMonth).year(this.scheduleExprYear).timezone(this.scheduleExprTimezone)
                    .start(this.scheduleExprStartDate).end(this.scheduleExprEndDate);

        }
        return scheduleExpression;
    }

    public CalendarBasedTimeout getCalendarTimeout() {
        if (this.calendarTimeout == null) {
            this.calendarTimeout = new CalendarBasedTimeout(this.getScheduleExpression());
        }
        return this.calendarTimeout;
    }

    public String getSecond() {
        return scheduleExprSecond;
    }

    public String getMinute() {
        return scheduleExprMinute;
    }

    public String getHour() {
        return scheduleExprHour;
    }

    public String getDayOfWeek() {
        return scheduleExprDayOfWeek;
    }

    public String getDayOfMonth() {
        return scheduleExprDayOfMonth;
    }

    public String getMonth() {
        return scheduleExprMonth;
    }

    public String getYear() {
        return scheduleExprYear;
    }

    public Date getStartDate() {
        return scheduleExprStartDate;
    }

    public Date getEndDate() {
        return scheduleExprEndDate;
    }

    public TimeoutMethod getTimeoutMethod() {
        return timeoutMethod;
    }

    public boolean isAutoTimer() {
        return autoTimer;
    }

    public String getTimezone() {
        return scheduleExprTimezone;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof CalendarTimerEntity == false) {
            return false;
        }
        CalendarTimerEntity other = (CalendarTimerEntity) obj;
        if (this.id == null) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        if (this.id == null) {
            return super.hashCode();
        }
        return this.id.hashCode();
    }

}
