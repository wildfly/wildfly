/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.timerservice;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.persistence.TimeoutMethod;
import org.jboss.as.ejb3.timerservice.schedule.CalendarBasedTimeout;

/**
 * Represents a {@link javax.ejb.Timer} which is created out a calendar expression
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class CalendarTimer extends TimerImpl {

    /**
     * The calendar based timeout for this timer
     */
    private final CalendarBasedTimeout calendarTimeout;

    /**
     * Represents whether this is an auto-timer or a normal
     * programmatically created timer
     */
    private final boolean autoTimer;

    private final Method timeoutMethod;

    public CalendarTimer(Builder builder, TimerServiceImpl timerService) {
        super(builder, timerService);

        this.autoTimer = builder.autoTimer;
        if (autoTimer) {
            assert builder.timeoutMethod != null;
            this.timeoutMethod = builder.timeoutMethod;
        } else {
            assert builder.timeoutMethod == null;
            this.timeoutMethod = null;
        }

        ScheduleExpression s = new ScheduleExpression();
        s.second(builder.scheduleExprSecond);
        s.minute(builder.scheduleExprMinute);
        s.hour(builder.scheduleExprHour);
        s.dayOfWeek(builder.scheduleExprDayOfWeek);
        s.dayOfMonth(builder.scheduleExprDayOfMonth);
        s.month(builder.scheduleExprMonth);
        s.year(builder.scheduleExprYear);
        s.start(builder.scheduleExprStartDate);
        s.end(builder.scheduleExprEndDate);
        s.timezone(builder.scheduleExprTimezone);
        this.calendarTimeout = new CalendarBasedTimeout(s);

        if (builder.nextDate == null && builder.newTimer) {
            // compute the next timeout (from "now")
            Calendar nextTimeout = this.calendarTimeout.getNextTimeout();
            if (nextTimeout != null) {
                this.nextExpiration = nextTimeout.getTime();
            }
        }
    }


    /**
     * {@inheritDoc}
     *
     * @see #getScheduleExpression()
     */
    @Override
    public ScheduleExpression getSchedule() throws IllegalStateException, EJBException {
        this.assertTimerState();
        return this.calendarTimeout.getScheduleExpression();
    }

    /**
     * This method is similar to {@link #getSchedule()}, except that this method does <i>not</i> check the timer state
     * and hence does <i>not</i> throw either {@link IllegalStateException} or {@link javax.ejb.NoSuchObjectLocalException}
     * or {@link javax.ejb.EJBException}.
     *
     * @return
     */
    public ScheduleExpression getScheduleExpression() {
        return this.calendarTimeout.getScheduleExpression();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCalendarTimer() throws IllegalStateException, EJBException {
        this.assertTimerState();
        return true;
    }

    /**
     * Returns the {@link CalendarBasedTimeout} corresponding to this
     * {@link CalendarTimer}
     *
     * @return
     */
    public CalendarBasedTimeout getCalendarTimeout() {
        return this.calendarTimeout;
    }

    /**
     * Returns true if this is an auto-timer. Else returns false.
     */
    @Override
    public boolean isAutoTimer() {
        return autoTimer;
    }

    /**
     * Returns the task which handles the timeouts on this {@link CalendarTimer}
     *
     * @see CalendarTimerTask
     */
    @Override
    protected TimerTask<?> getTimerTask() {
        return new CalendarTimerTask(this);
    }

    public Method getTimeoutMethod() {
        if (!this.autoTimer) {
            throw EjbLogger.EJB3_TIMER_LOGGER.failToInvokegetTimeoutMethod();
        }
        return this.timeoutMethod;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Makes sure that the timer is only run once after being restored.
     */
    public void handleRestorationCalculation() {
        if(nextExpiration == null) {
            return;
        }
        //next expiration in the future, we don't care
        if(nextExpiration.getTime() >= System.currentTimeMillis()) {
            return;
        }
        //just set the next expiration to 1ms in the past
        //this means it will run to catch up the missed expiration
        //and then the next calculated expiration will be in the future
        nextExpiration = new Date(System.currentTimeMillis() - 1);
    }

    public static class Builder extends TimerImpl.Builder {
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
        private Method timeoutMethod;

        public Builder setScheduleExprSecond(final String scheduleExprSecond) {
            this.scheduleExprSecond = scheduleExprSecond;
            return this;
        }

        public Builder setScheduleExprMinute(final String scheduleExprMinute) {
            this.scheduleExprMinute = scheduleExprMinute;
            return this;
        }

        public Builder setScheduleExprHour(final String scheduleExprHour) {
            this.scheduleExprHour = scheduleExprHour;
            return this;
        }

        public Builder setScheduleExprDayOfWeek(final String scheduleExprDayOfWeek) {
            this.scheduleExprDayOfWeek = scheduleExprDayOfWeek;
            return this;
        }

        public Builder setScheduleExprDayOfMonth(final String scheduleExprDayOfMonth) {
            this.scheduleExprDayOfMonth = scheduleExprDayOfMonth;
            return this;
        }

        public Builder setScheduleExprMonth(final String scheduleExprMonth) {
            this.scheduleExprMonth = scheduleExprMonth;
            return this;
        }

        public Builder setScheduleExprYear(final String scheduleExprYear) {
            this.scheduleExprYear = scheduleExprYear;
            return this;
        }

        public Builder setScheduleExprStartDate(final Date scheduleExprStartDate) {
            this.scheduleExprStartDate = scheduleExprStartDate;
            return this;
        }

        public Builder setScheduleExprEndDate(final Date scheduleExprEndDate) {
            this.scheduleExprEndDate = scheduleExprEndDate;
            return this;
        }

        public Builder setScheduleExprTimezone(final String scheduleExprTimezone) {
            this.scheduleExprTimezone = scheduleExprTimezone;
            return this;
        }

        public Builder setAutoTimer(final boolean autoTimer) {
            this.autoTimer = autoTimer;
            return this;
        }

        public Builder setTimeoutMethod(final Method timeoutMethod) {
            this.timeoutMethod = timeoutMethod;
            return this;
        }

        public CalendarTimer build(final TimerServiceImpl timerService) {
            return new CalendarTimer(this, timerService);
        }
    }

    /**
     * Returns the {@link java.lang.reflect.Method}, represented by the {@link org.jboss.as.ejb3.timerservice.persistence.TimeoutMethod}
     * <p>
     * Note: This method uses the {@link Thread#getContextClassLoader()} to load the
     * relevant classes while getting the {@link java.lang.reflect.Method}
     * </p>
     *
     * @param timeoutMethodInfo  The timeout method
     * @param classLoader The class loader
     * @return timeout method matching {@code timeoutMethodInfo}
     */
    public static Method getTimeoutMethod(TimeoutMethod timeoutMethodInfo, ClassLoader classLoader) {
        if(timeoutMethodInfo == null) {
            return null;
        }
        Class<?> timeoutMethodDeclaringClass;
        try {
            timeoutMethodDeclaringClass = Class.forName(timeoutMethodInfo.getDeclaringClass(), false, classLoader);
        } catch (ClassNotFoundException cnfe) {
            throw EjbLogger.EJB3_TIMER_LOGGER.failToLoadDeclaringClassOfTimeOut(timeoutMethodInfo.getDeclaringClass());
        }

        // now start looking for the method
        String timeoutMethodName = timeoutMethodInfo.getMethodName();
        Class<?> klass = timeoutMethodDeclaringClass;
        while (klass != null) {
            Method[] methods = klass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(timeoutMethodName)) {
                    if (timeoutMethodInfo.hasTimerParameter()) {
                        if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == Timer.class) {
                            return method;
                        }
                    } else if (method.getParameterCount() == 0) {
                        return method;
                    }
                } // end: method name matching
            } // end: all methods in current klass
            klass = klass.getSuperclass();

        }
        // no match found
        return null;
    }

    /**
     * {@inheritDoc}. For calendar-based timer, the string output also includes its schedule expression value.
     *
     * @return a string representation of calendar-based timer
     */
    @Override
    public String toString() {
        return super.toString() + " " + getScheduleExpression();
    }
}
