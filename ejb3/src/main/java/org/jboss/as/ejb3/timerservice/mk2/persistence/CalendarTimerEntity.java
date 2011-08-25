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
package org.jboss.as.ejb3.timerservice.mk2.persistence;

import org.jboss.as.ejb3.timerservice.schedule.CalendarBasedTimeout;
import org.jboss.as.ejb3.timerservice.mk2.CalendarTimer;

import javax.ejb.ScheduleExpression;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * CalendarTimerEntity
 *
 * @author Jaikiran Pai
 * @author Stuart Douglas
 */
public class CalendarTimerEntity extends TimerEntity {

    private transient ScheduleExpression scheduleExpression;

    private transient CalendarBasedTimeout calendarTimeout;

    private String scheduleExprSecond;

    private String scheduleExprMinute;

    private String scheduleExprHour;

    private String scheduleExprDayOfWeek;

    private String scheduleExprDayOfMonth;

    private String scheduleExprMonth;

    private String scheduleExprYear;

    private Date scheduleExprStartDate;

    private Date scheduleExprEndDate;

    private String scheduleExprTimezone;

    private boolean autoTimer;

    private TimeoutMethod timeoutMethod;

    public CalendarTimerEntity() {

    }

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
                    .dayOfMonth(this.scheduleExprDayOfMonth).month(this.scheduleExprMonth).year(this.scheduleExprYear).timezone(this.scheduleExprTimezone);

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

    public void setStartDate(Date start) {
        this.scheduleExprStartDate = start;
    }

    public Date getEndDate() {
        return scheduleExprEndDate;
    }

    public void setEndDate(Date end) {
        this.scheduleExprEndDate = end;
    }

    public TimeoutMethod getTimeoutMethod() {
        return timeoutMethod;
    }

    public void setTimeoutMethod(TimeoutMethod timeoutMethod) {
        this.timeoutMethod = timeoutMethod;
    }

    public boolean isAutoTimer() {
        return autoTimer;
    }

    public void setAutoTimer(boolean autoTimer) {
        this.autoTimer = autoTimer;
    }

    public String getTimezone() {
        return scheduleExprTimezone;
    }

    public void setTimezone(String timezone) {
        this.scheduleExprTimezone = timezone;
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
