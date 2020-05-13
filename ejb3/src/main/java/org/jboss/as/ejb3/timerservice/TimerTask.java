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
package org.jboss.as.ejb3.timerservice;

import java.util.Date;

import org.wildfly.security.manager.WildFlySecurityManager;

import static org.jboss.as.ejb3.logging.EjbLogger.EJB3_TIMER_LOGGER;

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
            throw EJB3_TIMER_LOGGER.timerIsNull();
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
        ClassLoader old = WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(getClass());
        try {
            final TimerImpl timer = timerService.getTimer(timerId);
            try {
                if (timer == null || cancelled) {
                    EJB3_TIMER_LOGGER.debugf("Timer task was cancelled for %s", timer);
                    return;
                }

                Date now = new Date();
                EJB3_TIMER_LOGGER.debugf("Timer task invoked at: %s for timer %s", now, timer);

                //we lock the timer for this check, because if a cancel is in progress or a running timeout is about to finish
                //and try to update the timer state after finish the inTimeout method,
                //we do not want to do the isActive check, but wait for the other transaction to finish
                //one way or another
                timer.lock();

                try {
                    // If a retry thread is in progress, we don't want to allow another
                    // interval to execute until the retry is complete. See JIRA-1926.
                    if (timer.isInRetry()) {
                        EJB3_TIMER_LOGGER.skipInvokeTimeoutDuringRetry(timer, now);
                        // compute the next timeout, See JIRA AS7-2995.
                        timer.setNextTimeout(calculateNextTimeout(timer));
                        timerService.persistTimer(timer, false);
                        scheduleTimeoutIfRequired(timer);
                        return;
                    }

                    // Check whether the timer is running local
                    // If the recurring timer running longer than the interval is, we don't want to allow another
                    // execution until it is complete. See JIRA AS7-3119
                    if (timer.getState() == TimerState.IN_TIMEOUT || timer.getState() == TimerState.RETRY_TIMEOUT) {
                        EJB3_TIMER_LOGGER.skipOverlappingInvokeTimeout(timer, now);

                        if (EJB3_TIMER_LOGGER.isDebugEnabled()) {
                            // WFLY-10542 log thread stack trace which is processing timer task in debug level to diagnose timer overlap
                            Thread otherThread = timer.getExecutingThread();
                            // can be null for clustered timers if the timer is executing on another node.
                            if (otherThread != null) {
                                final StringBuilder debugMsg = new StringBuilder()
                                        .append("Thread: ")
                                        .append(otherThread.getName())
                                        .append(" Id: ")
                                        .append(otherThread.getId())
                                        .append(" of group ")
                                        .append(otherThread.getThreadGroup())
                                        .append(" is in state: ")
                                        .append(otherThread.getState());
                                for (StackTraceElement ste : otherThread.getStackTrace()) {
                                    debugMsg.append(System.lineSeparator()).append(ste.toString());
                                }
                                EJB3_TIMER_LOGGER.debugf(debugMsg.toString());
                            }
                        }

                        Date newD = this.calculateNextTimeout(timer);
                        timer.setNextTimeout(newD);
                        timerService.persistTimer(timer, false);
                        scheduleTimeoutIfRequired(timer);
                        return;
                    }

                    // Check whether we want to run the timer
                    if (!timerService.shouldRun(timer)) {
                        EJB3_TIMER_LOGGER.debugf("Skipping execution of timer for %s as it is being run on another node or the execution is suppressed by configuration", timer.getTimedObjectId());
                        timer.setNextTimeout(calculateNextTimeout(timer));
                        scheduleTimeoutIfRequired(timer);
                        return;
                    }

                    if (!timer.isActive()) {
                        EJB3_TIMER_LOGGER.debug("Timer is not active, skipping this scheduled execution at: " + now + "for " + timer);
                        return;
                    }
                    // set the current date as the "previous run" of the timer.
                    timer.setPreviousRun(new Date());
                    Date nextTimeout = this.calculateNextTimeout(timer);
                    timer.setNextTimeout(nextTimeout);
                    // change the state to mark it as in timeout method
                    timer.setTimerState(TimerState.IN_TIMEOUT, Thread.currentThread());

                    // persist changes
                    timerService.persistTimer(timer, false);

                } finally {
                    timer.unlock();
                }
                try {
                    // invoke timeout
                    this.callTimeout(timer);
                } catch (Exception e) {
                    EJB3_TIMER_LOGGER.errorInvokeTimeout(timer, e);
                    try {
                        EJB3_TIMER_LOGGER.timerRetried(timer);
                        retryTimeout(timer);
                    } catch (Exception retryException) {
                        // that's it, we can't do anything more. Let's just log the exception
                        // and return
                        EJB3_TIMER_LOGGER.errorDuringRetryTimeout(timer, retryException);
                    }
                } finally {
                    this.postTimeoutProcessing(timer);
                }
            } catch (Exception e) {
                EJB3_TIMER_LOGGER.exceptionRunningTimerTask(timer, timedObjectId, e);
            }
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(old);
        }
    }

    protected void scheduleTimeoutIfRequired(TimerImpl timer) {
    }

    protected void callTimeout(TimerImpl timer) throws Exception {
        invokeBeanMethod(timer);
    }

    protected void invokeBeanMethod(TimerImpl timer) throws Exception {
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

    /**
     * After a timeout failed the timer need to retried.
     * The method must lock the timer for state check and update but not during callTimeout run.
     *
     * @param timer timer to retry and state updates
     * @throws Exception
     */
    protected void retryTimeout(TimerImpl timer) throws Exception {
        boolean callTimeout = false;

        timer.lock();
        try {
            if (timer.isActive()) {
                EJB3_TIMER_LOGGER.retryingTimeout(timer);
                timer.setTimerState(TimerState.RETRY_TIMEOUT, Thread.currentThread());
                timerService.persistTimer(timer, false);
                callTimeout = true;
            } else {
                EJB3_TIMER_LOGGER.timerNotActive(timer);
            }
        } finally {
            timer.unlock();
        }
        if(callTimeout) {
            this.callTimeout(timer);
        }
    }

    /**
     * After running the timer calculate the new state or expire the timer and persist it if changed.
     * The method must lock the timer for state check and updates if overridden.
     *
     * @param timer timer to post processing and persist
     */
    protected void postTimeoutProcessing(TimerImpl timer) throws InterruptedException {
        timer.lock();
        try {
            TimerState timerState = timer.getState();
            if (timerState != TimerState.CANCELED
                    && timerState != TimerState.EXPIRED) {
                if (timer.getInterval() == 0) {
                    timerService.expireTimer(timer);
                } else {
                    timer.setTimerState(TimerState.ACTIVE, null);
                }
                timerService.persistTimer(timer, false);
            }
        } finally {
            timer.unlock();
        }
    }

    public void cancel() {
        cancelled = true;
    }
}
