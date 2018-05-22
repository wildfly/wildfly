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


    /**
     * Constructs a {@link CalendarTimer}
     *
     * @param id              The id of this timer
     * @param timerService    The timer service to which this timer belongs
     * @param calendarTimeout The {@link CalendarBasedTimeout} from which this {@link CalendarTimer} is being created
     * @param info            The serializable info which will be made available through {@link javax.ejb.Timer#getInfo()}
     * @param persistent      True if this timer is persistent. False otherwise
     * @param timeoutMethod   If this is a non-null value, then this {@link CalendarTimer} is marked as an auto-timer.
     *                        This <code>timeoutMethod</code> is then considered as the name of the timeout method which has to
     *                        be invoked when this timer times out.
     */
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

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * A {@link javax.ejb.Timer} is equal to another {@link javax.ejb.Timer} if their
     * {@link javax.ejb.TimerHandle}s are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.handle == null) {
            return false;
        }
        if (!(obj instanceof CalendarTimer)) {
            return false;
        }
        CalendarTimer otherTimer = (CalendarTimer) obj;
        return this.handle.equals(otherTimer.getTimerHandle());
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
     * @return
     */
    public static Method getTimeoutMethod(TimeoutMethod timeoutMethodInfo, ClassLoader classLoader) {
        if(timeoutMethodInfo == null) {
            return null;
        }
        String declaringClass = timeoutMethodInfo.getDeclaringClass();
        Class<?> timeoutMethodDeclaringClass = null;
        try {
            timeoutMethodDeclaringClass = Class.forName(declaringClass, false, classLoader);
        } catch (ClassNotFoundException cnfe) {
            throw EjbLogger.EJB3_TIMER_LOGGER.failToLoadDeclaringClassOfTimeOut(declaringClass);
        }

        String timeoutMethodName = timeoutMethodInfo.getMethodName();
        String[] timeoutMethodParams = timeoutMethodInfo.getMethodParams();
        // load the method param classes
        Class<?>[] timeoutMethodParamTypes = new Class<?>[]
                {};
        if (timeoutMethodParams != null) {
            timeoutMethodParamTypes = new Class<?>[timeoutMethodParams.length];
            int i = 0;
            for (String paramClassName : timeoutMethodParams) {
                Class<?> methodParamClass = null;
                try {
                    methodParamClass = Class.forName(paramClassName, false, classLoader);
                } catch (ClassNotFoundException cnfe) {
                    throw EjbLogger.EJB3_TIMER_LOGGER.failedToLoadTimeoutMethodParamClass(cnfe, paramClassName);
                }
                timeoutMethodParamTypes[i++] = methodParamClass;
            }
        }
        // now start looking for the method
        Class<?> klass = timeoutMethodDeclaringClass;
        while (klass != null) {
            Method[] methods = klass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(timeoutMethodName)) {
                    Class<?>[] methodParamTypes = method.getParameterTypes();
                    // param length doesn't match
                    if (timeoutMethodParamTypes.length != methodParamTypes.length) {
                        continue;
                    }
                    boolean match = true;
                    for (int i = 0; i < methodParamTypes.length; i++) {
                        // param type doesn't match
                        if (!timeoutMethodParamTypes[i].equals(methodParamTypes[i])) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        // match found
                        return method;
                    }
                }
            }
            klass = klass.getSuperclass();

        }
        // no match found
        return null;
    }
}
