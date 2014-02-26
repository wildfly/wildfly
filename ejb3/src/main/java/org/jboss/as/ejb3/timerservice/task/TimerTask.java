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
package org.jboss.as.ejb3.timerservice.task;

import java.util.Date;

import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceImpl;
import org.jboss.as.ejb3.timerservice.TimerState;
import org.jboss.as.ejb3.timerservice.spi.BeanRemovedException;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * A timer task which will be invoked at appropriate intervals based on a {@link javax.ejb.Timer}
 * schedule.
 * <p/>
 * <p>
 * A {@link TimerTask} is responsible for invoking the timeout method on the target, through
 * the use of {@link org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker}
 * </p>
 * <p>
 * For calendar timers, this {@link TimerTask} is additionally responsible for creating and
 * scheduling the next round of timer task.
 * </p>
 *
 * @author Jaikiran Pai
 * @author Wolf-Dieter Fink
 * @version $Revision: $
 */
public class TimerTask<T extends TimerImpl> implements Runnable {

    protected final String timedObjectId;
    protected final String timerId;


    /**
     * {@link org.jboss.as.ejb3.timerservice.TimerServiceImpl} to which this {@link TimerTask} belongs
     */
    protected final TimerServiceImpl timerService;

    private volatile boolean cancelled = false;

    /**
     * Creates a {@link TimerTask} for the timer
     *
     * @param timer The timer for which this task is being created.
     * @throws IllegalArgumentException If the passed timer is null
     */
    public TimerTask(T timer) {
        if (timer == null) {
            throw MESSAGES.timerIsNull();
        }

        this.timedObjectId = timer.getTimedObjectId();
        timerId = timer.getId();
        timerService = timer.getTimerService();
    }

    /**
     * Invokes the timeout method through the {@link org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker} corresponding
     * to the {@link org.jboss.as.ejb3.timerservice.TimerImpl} to which this {@link TimerTask} belongs.
     * <p>
     * This method also sets other attributes on the {@link org.jboss.as.ejb3.timerservice.TimerImpl} including the
     * next timeout of the timer and the timer state.
     * </p>
     * <p>
     * Additionally, for calendar timers, this method even schedules the next timeout timer task
     * before calling the timeout method for the current timeout.
     * </p>
     */
    @Override
    public void run() {
        try {
            TimerImpl timer = timerService.getTimer(timedObjectId, timerId);
            if (cancelled) {
                ROOT_LOGGER.debug("Timer task was cancelled for " + timer);
                return;
            }

            Date now = new Date();
            if (ROOT_LOGGER.isDebugEnabled()) {
                ROOT_LOGGER.debug("Timer task invoked at: " + now + " for timer " + timer);
            }

            // If a retry thread is in progress, we don't want to allow another
            // interval to execute until the retry is complete. See JIRA-1926.
            if (timer.isInRetry()) {
                ROOT_LOGGER.skipInvokeTimeoutDuringRetry(timer.getTimedObjectId(), timer.getId(), now);
                // compute the next timeout, See JIRA AS7-2995.
                timer.setNextTimeout(calculateNextTimeout(timer));
                timerService.persistTimer(timer, false);
                scheduleTimeoutIfRequired(timer);
                return;
            }
            // If the recurring timer running longer than the interval is, we don't want to allow another
            // execution until it is complete. See JIRA AS7-3119
            if (timer.getState() == TimerState.IN_TIMEOUT && !timer.isCalendarTimer()) {
                ROOT_LOGGER.skipOverlappingInvokeTimeout(timer.getTimedObjectId(), timer.getId(), now);
                timer.setNextTimeout(this.calculateNextTimeout(timer));
                timerService.persistTimer(timer, false);
                return;
            }

            // If a retry thread is in progress, we don't want to allow another
            // interval to execute until the retry is complete. See JIRA-1926.
            if (timer.isInRetry()) {
                ROOT_LOGGER.debug("Timer in retry mode, skipping this scheduled execution at: " + now);
                // compute the next timeout, See JIRA AS7-2995.
                timer.setNextTimeout(calculateNextTimeout(timer));
                timerService.persistTimer(timer, false);
                scheduleTimeoutIfRequired(timer);
                return;
            }
            // If the recurring timer running longer than the interval is, we don't want to allow another
            // execution until it is complete. See JIRA AS7-3119
            if (timer.getState() == TimerState.IN_TIMEOUT && !timer.isCalendarTimer()) {
                ROOT_LOGGER.skipOverlappingInvokeTimeout(timer.getTimedObjectId(), timer.getId(), now);
                timer.setNextTimeout(this.calculateNextTimeout(timer));
                timerService.persistTimer(timer, false);
                return;
            }

            //we lock the timer for this check, because if a cancel is in progress then
            //we do not want to do the isActive check, but wait for the cancelling transaction to finish
            //one way or another
            timer.lock();
            try {
                if (!timer.isActive()) {
                    ROOT_LOGGER.debug("Timer is not active, skipping this scheduled execution at: " + now + "for " + timer);
                    return;
                }
                // set the current date as the "previous run" of the timer.
                timer.setPreviousRun(new Date());
                Date nextTimeout = this.calculateNextTimeout(timer);
                timer.setNextTimeout(nextTimeout);
                // change the state to mark it as in timeout method
                timer.setTimerState(TimerState.IN_TIMEOUT);

                // persist changes
                timerService.persistTimer(timer, false);

            } finally {
                timer.unlock();
            }
            try {
                // invoke timeout
                this.callTimeout(timer);
            } catch (BeanRemovedException e) {
                ROOT_LOGGER.debugf("Removing timer %s as EJB has been removed ", timer);
                timer.cancel();
            } catch (Exception e) {
                ROOT_LOGGER.errorInvokeTimeout(timer, e);
                try {
                    ROOT_LOGGER.timerRetried(timer);
                    retryTimeout(timer);
                } catch (Exception retryException) {
                    // that's it, we can't do anything more. Let's just log the exception
                    // and return
                    ROOT_LOGGER.errorDuringRetryTimeout(timer, retryException);
                }
            } finally {
                this.postTimeoutProcessing(timer);
                //if it has expired we need to persist it
                timerService.persistTimer(timer, false);
            }
        } catch (Exception e) {
            ROOT_LOGGER.exceptionRunningTimerTask(timerId, timedObjectId, e);
        }
    }

    protected void scheduleTimeoutIfRequired(TimerImpl timer) {
    }

    protected void callTimeout(TimerImpl timer) throws Exception {
        timerService.getInvoker().callTimeout(timer);
    }

    protected Date calculateNextTimeout(TimerImpl timer) {
        long intervalDuration = timer.getInterval();
        if (intervalDuration > 0) {
            long now = new Date().getTime();
            long nextExpiration = timer.getNextExpiration().getTime();
            // compute skipped number of interval
            int periods = (int) ((now - nextExpiration) / intervalDuration);
            // compute the next timeout date
            return new Date(nextExpiration + (periods * intervalDuration) + intervalDuration);
        }
        return null;

    }

    protected void retryTimeout(TimerImpl timer) throws Exception {
        if (timer.isActive()) {
            ROOT_LOGGER.retryingTimeout(timer);
            timer.setTimerState(TimerState.RETRY_TIMEOUT);
            timerService.persistTimer(timer, false);

            this.callTimeout(timer);
        } else {
            ROOT_LOGGER.timerNotActive(timer);
        }
    }

    protected void postTimeoutProcessing(TimerImpl timer) {
        timer.lock();
        try {
            TimerState timerState = timer.getState();
            if (timerState != TimerState.CANCELED
                    && timerState != TimerState.EXPIRED) {
                if (timer.getInterval() == 0) {
                    timerService.expireTimer(timer);
                } else {
                    timer.setTimerState(TimerState.ACTIVE);
                    // persist changes
                    timerService.persistTimer(timer, false);
                }
            }

        } finally {
            timer.unlock();
        }
    }

    public void cancel() {
        cancelled = true;
    }
}
