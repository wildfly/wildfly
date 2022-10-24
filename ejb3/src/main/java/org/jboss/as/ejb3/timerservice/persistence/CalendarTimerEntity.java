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
import jakarta.ejb.ScheduleExpression;

import org.jboss.as.ejb3.timerservice.CalendarTimer;

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
    private static final long serialVersionUID = -8641876649577480976L;

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

        ScheduleExpression scheduleExpression = calendarTimer.getScheduleExpression();
        this.scheduleExprSecond = scheduleExpression.getSecond();
        this.scheduleExprMinute = scheduleExpression.getMinute();
        this.scheduleExprHour = scheduleExpression.getHour();
        this.scheduleExprDayOfMonth = scheduleExpression.getDayOfMonth();
        this.scheduleExprMonth = scheduleExpression.getMonth();
        this.scheduleExprDayOfWeek = scheduleExpression.getDayOfWeek();
        this.scheduleExprYear = scheduleExpression.getYear();
        this.scheduleExprStartDate = scheduleExpression.getStart();
        this.scheduleExprEndDate = scheduleExpression.getEnd();
        this.scheduleExprTimezone = scheduleExpression.getTimezone();

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
