/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.timerservice;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.jboss.as.ejb3.timerservice.schedule.CalendarBasedTimeout;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;

/**
 * CalendarTimerTask
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class CalendarTimerTask extends TimerTask {

    public CalendarTimerTask(CalendarTimer calendarTimer) {
        super(calendarTimer);
    }

    @Override
    protected void callTimeout(TimerImpl timer) throws Exception {
        // if we have any more schedules remaining, then schedule a new task
        if (timer.getNextExpiration() != null && !timer.isInRetry()) {
            timer.scheduleTimeout(false);
        }
        invokeBeanMethod(timer);
    }

    @Override
    protected void invokeBeanMethod(TimerImpl timer) throws Exception {
        // finally invoke the timeout method through the invoker
        if (timer.isAutoTimer()) {
            CalendarTimer calendarTimer = (CalendarTimer) timer;
            TimedObjectInvoker invoker = this.timerService.getInvoker();
            // call the timeout method
            invoker.callTimeout(calendarTimer, calendarTimer.getTimeoutMethod());
        } else {
            this.timerService.getInvoker().callTimeout(timer);
        }
    }

    @Override
    protected Date calculateNextTimeout(TimerImpl timer) {
        // The next timeout for the calendar timer will have to be computed using the
        // current "nextExpiration"
        Date currentTimeout = timer.getNextExpiration();
        if (currentTimeout == null) {
            return null;
        }
        Calendar nextTimeout = new GregorianCalendar();
        nextTimeout.setTime(currentTimeout);

        CalendarBasedTimeout timeout = ((CalendarTimer) timer).getCalendarTimeout();
        Date now = new Date();
        do {
            nextTimeout = timeout.getNextTimeout(nextTimeout);
            // Ensure next timeout is in the future
        } while ((nextTimeout != null) && nextTimeout.getTime().before(now));

        return (nextTimeout != null) ? nextTimeout.getTime() : null;
    }

    @Override
    protected void scheduleTimeoutIfRequired(TimerImpl timer) {
        if (timer.getNextExpiration() != null) {
            timer.scheduleTimeout(false);
        }
    }

    @Override
    protected void postTimeoutProcessing(TimerImpl timer) throws InterruptedException {
        timer.lock();
        try {
            final TimerState timerState = timer.getState();
            if (timerState != TimerState.CANCELED
                    && timerState != TimerState.EXPIRED) {
                if (timer.getNextExpiration() == null) {
                    timerService.expireTimer(timer);
                } else {
                    timer.setTimerState(TimerState.ACTIVE, null);
                    // persist changes
                    timerService.persistTimer(timer, false);
                }
            }
        } finally {
            timer.unlock();
        }
    }

}
