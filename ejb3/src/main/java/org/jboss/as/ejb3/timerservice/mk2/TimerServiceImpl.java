/*
c * JBoss, Home of Professional Open Source.
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
package org.jboss.as.ejb3.timerservice.mk2;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.singleton.SingletonComponent;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.ejb3.timerservice.schedule.CalendarBasedTimeout;
import org.jboss.as.ejb3.timerservice.api.TimerService;
import org.jboss.as.ejb3.timerservice.mk2.persistence.CalendarTimerEntity;
import org.jboss.as.ejb3.timerservice.mk2.persistence.TimeoutMethod;
import org.jboss.as.ejb3.timerservice.mk2.persistence.TimerEntity;
import org.jboss.as.ejb3.timerservice.mk2.persistence.TimerPersistence;
import org.jboss.as.ejb3.timerservice.mk2.task.TimerTask;
import org.jboss.as.ejb3.timerservice.spi.ScheduleTimer;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;


/**
 * MK2 implementation of EJB3.1 {@link TimerService}
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class TimerServiceImpl implements TimerService {
    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(TimerServiceImpl.class);

    /**
     * The {@link TimedObjectInvoker} which is responsible for invoking the timeout
     * method
     */
    private final TimedObjectInvoker invoker;

    /**
     * Used for persistent timers
     */
    private final TimerPersistence timerPersistence;

    /**
     * Transaction manager
     */
    private final TransactionManager transactionManager;

    /**
     * For running timeout tasks
     */
    private final ExecutorService executorService;

    /**
     * The timer
     */
    private final java.util.Timer timer;

    /**
     * All non-persistent timers which were created by this {@link TimerService}
     */
    private final Map<TimerHandle, TimerImpl> nonPersistentTimers = new ConcurrentHashMap<TimerHandle, TimerImpl>();

    private final Map<TimerHandle, TimerImpl> persistentWaitingOnTxCompletionTimers = new ConcurrentHashMap<TimerHandle, TimerImpl>();

    /**
     * Holds the {@link java.util.concurrent.Future} of each of the timer tasks that have been scheduled
     */
    private final Map<TimerHandle, java.util.TimerTask> scheduledTimerFutures = new ConcurrentHashMap<TimerHandle, java.util.TimerTask>();

    private final EJBComponent component;


    /**
     * Creates a {@link TimerServiceImpl}
     *
     *
     * @param invoker            The {@link org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker} responsible for invoking the timeout method
     * @param timerPersistence   The persistent timer store
     * @param transactionManager Transaction manager responsible for managing the transactional timer service
     * @param executor           Executor service responsible for creating scheduled timer tasks
     * @param component
     * @throws IllegalArgumentException If either of the passed param is null
     */
    public TimerServiceImpl(final java.util.Timer timer, TimedObjectInvoker invoker, final TimerPersistence timerPersistence, TransactionManager transactionManager,
                            ExecutorService executor, final EJBComponent component) {
        this.component = component;
        if (invoker == null) {
            throw new IllegalArgumentException("Invoker cannot be null");
        }
        if (transactionManager == null) {
            throw new IllegalArgumentException("Transaction manager cannot be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor cannot be null");
        }

        this.invoker = invoker;
        this.timerPersistence = timerPersistence;
        this.transactionManager = transactionManager;
        this.executorService = executor;
        this.timer = timer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule) throws IllegalArgumentException,
            IllegalStateException, EJBException {
        handleLifecycleCallback();
        return this.createCalendarTimer(schedule, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig timerConfig)
            throws IllegalArgumentException, IllegalStateException, EJBException {
        handleLifecycleCallback();
        Serializable info = timerConfig == null ? null : timerConfig.getInfo();
        boolean persistent = timerConfig == null || timerConfig.isPersistent();
        return this.createCalendarTimer(schedule, info, persistent, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createIntervalTimer(Date initialExpiration, long intervalDuration, TimerConfig timerConfig)
            throws IllegalArgumentException, IllegalStateException, EJBException {
        handleLifecycleCallback();
        if (initialExpiration == null) {
            throw new IllegalArgumentException("initialExpiration cannot be null while creating a timer");
        }
        if (initialExpiration.getTime() < 0) {
            throw new IllegalArgumentException("initialExpiration.getTime() cannot be negative while creating a timer");
        }
        if (intervalDuration < 0) {
            throw new IllegalArgumentException("intervalDuration cannot be negative while creating a timer");
        }
        return this.createTimer(initialExpiration, intervalDuration, timerConfig.getInfo(), timerConfig.isPersistent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createIntervalTimer(long initialDuration, long intervalDuration, TimerConfig timerConfig)
            throws IllegalArgumentException, IllegalStateException, EJBException {
        handleLifecycleCallback();
        if (initialDuration < 0) {
            throw new IllegalArgumentException("initialDuration cannot be negative while creating interval timer");
        }
        if (intervalDuration < 0) {
            throw new IllegalArgumentException("intervalDuration cannot be negative while creating interval timer");
        }

        return this.createIntervalTimer(new Date(System.currentTimeMillis() + initialDuration), intervalDuration, timerConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createSingleActionTimer(Date expiration, TimerConfig timerConfig) throws IllegalArgumentException,
            IllegalStateException, EJBException {
        handleLifecycleCallback();
        if (expiration == null) {
            throw new IllegalArgumentException("expiration cannot be null while creating a single action timer");
        }
        if (expiration.getTime() < 0) {
            throw new IllegalArgumentException(
                    "expiration.getTime() cannot be negative while creating a single action timer");
        }
        return this.createTimer(expiration, 0, timerConfig.getInfo(), timerConfig.isPersistent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createSingleActionTimer(long duration, TimerConfig timerConfig) throws IllegalArgumentException,
            IllegalStateException, EJBException {
        handleLifecycleCallback();
        if (duration < 0)
            throw new IllegalArgumentException("duration cannot be negative while creating single action timer");

        return createTimer(new Date(System.currentTimeMillis() + duration), 0, timerConfig.getInfo(), timerConfig
                .isPersistent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(long duration, Serializable info) throws IllegalArgumentException, IllegalStateException,
            EJBException {
        handleLifecycleCallback();
        if (duration < 0)
            throw new IllegalArgumentException("Duration cannot negative while creating the timer");

        return createTimer(new Date(System.currentTimeMillis() + duration), 0, info, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(Date expiration, Serializable info) throws IllegalArgumentException, IllegalStateException,
            EJBException {
        handleLifecycleCallback();
        if (expiration == null) {
            throw new IllegalArgumentException("Expiration date cannot be null while creating a timer");
        }
        if (expiration.getTime() < 0) {
            throw new IllegalArgumentException("expiration.getTime() cannot be negative while creating a timer");
        }
        return this.createTimer(expiration, 0, info, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(long initialDuration, long intervalDuration, Serializable info)
            throws IllegalArgumentException, IllegalStateException, EJBException {
        handleLifecycleCallback();
        if (initialDuration < 0) {
            throw new IllegalArgumentException("Initial duration cannot be negative while creating timer");
        }
        if (intervalDuration < 0) {
            throw new IllegalArgumentException("Interval cannot be negative while creating timer");
        }
        return this.createTimer(new Date(System.currentTimeMillis() + initialDuration), intervalDuration, info, true);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(Date initialExpiration, long intervalDuration, Serializable info)
            throws IllegalArgumentException, IllegalStateException, EJBException {
        handleLifecycleCallback();
        if (initialExpiration == null) {
            throw new IllegalArgumentException("intial expiration date cannot be null while creating a timer");
        }
        if (initialExpiration.getTime() < 0) {
            throw new IllegalArgumentException("expiration.getTime() cannot be negative while creating a timer");
        }
        if (intervalDuration < 0) {
            throw new IllegalArgumentException("interval duration cannot be negative while creating timer");
        }
        return this.createTimer(initialExpiration, intervalDuration, info, true);
    }

    @Override
    public org.jboss.as.ejb3.timerservice.api.Timer loadAutoTimer(ScheduleExpression schedule, Method timeoutMethod) {

        return this.createCalendarTimer(schedule, null, true, timeoutMethod);
    }

    @Override
    public org.jboss.as.ejb3.timerservice.api.Timer loadAutoTimer(ScheduleExpression schedule,
                                                               TimerConfig timerConfig, Method timeoutMethod) {
        return this.createCalendarTimer(schedule, timerConfig.getInfo(), timerConfig.isPersistent(), timeoutMethod);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Timer> getTimers() throws IllegalStateException, EJBException {
        handleLifecycleCallback();
        Set<Timer> activeTimers = new HashSet<Timer>();
        // get all active non-persistent timers for this timerservice
        for (TimerImpl timer : this.nonPersistentTimers.values()) {
            if (timer != null && timer.isActive()) {
                activeTimers.add(timer);
            }
        }
        // get all active timers which are persistent, but haven't yet been
        // persisted (waiting for tx to complete)
        for (TimerImpl timer : this.persistentWaitingOnTxCompletionTimers.values()) {
            if (timer != null && timer.isActive()) {
                activeTimers.add(timer);
            }
        }

        // now get all active persistent timers for this timerservice
        activeTimers.addAll(this.getActiveTimers());
        return activeTimers;
    }

    /**
     * Create a {@link javax.ejb.Timer}
     *
     * @param initialExpiration The {@link java.util.Date} at which the first timeout should occur.
     *                          <p>If the date is in the past, then the timeout is triggered immediately
     *                          when the timer moves to {@link TimerState#ACTIVE}</p>
     * @param intervalDuration  The interval (in milli seconds) between consecutive timeouts for the newly created timer.
     *                          <p>Cannot be a negative value. A value of 0 indicates a single timeout action</p>
     * @param info              {@link java.io.Serializable} info that will be made available through the newly created timer's {@link javax.ejb.Timer#getInfo()} method
     * @param persistent        True if the newly created timer has to be persistent
     * @return Returns the newly created timer
     * @throws IllegalArgumentException If <code>initialExpiration</code> is null or <code>intervalDuration</code> is negative
     * @throws IllegalStateException    If this method was invoked during a lifecycle callback on the EJB
     */
    private Timer createTimer(Date initialExpiration, long intervalDuration, Serializable info, boolean persistent) {
        if (this.isLifecycleCallbackInvocation() && !this.isSingletonBeanInvocation()) {
            throw new IllegalStateException("Creation of timers is not allowed during lifecycle callback of non-singleton EJBs");
        }
        if (initialExpiration == null) {
            throw new IllegalArgumentException("initial expiration is null");
        }
        if (intervalDuration < 0) {
            throw new IllegalArgumentException("interval duration is negative");
        }

        // create an id for the new timer instance
        UUID uuid = UUID.randomUUID();
        // create the timer
        TimerImpl timer = new TimerImpl(uuid.toString(), this, initialExpiration, intervalDuration, info, persistent);
        // if it's persistent, then save it
        if (persistent) {
            this.persistTimer(timer);
        }
        // now "start" the timer. This involves, moving the timer to an ACTIVE state
        // and scheduling the timer task
        this.startTimer(timer);

        this.addTimer(timer);
        // return the newly created timer
        return timer;
    }

    /**
     * Creates a calendar based {@link javax.ejb.Timer}
     *
     * @param schedule   The {@link javax.ejb.ScheduleExpression} which will be used for creating scheduled timer tasks
     *                   for a calendar based timer
     * @param info       {@link java.io.Serializable} info that will be made available through the newly created timer's {@link javax.ejb.Timer#getInfo()} method
     * @param persistent True if the newly created timer has to be persistent
     * @return Returns the newly created timer
     * @throws IllegalArgumentException If the passed <code>schedule</code> is null
     * @throws IllegalStateException    If this method was invoked during a lifecycle callback on the EJB
     */
    private org.jboss.as.ejb3.timerservice.api.Timer createCalendarTimer(ScheduleExpression schedule,
                                                                      Serializable info, boolean persistent, Method timeoutMethod) {
        if (this.isLifecycleCallbackInvocation() && !this.isSingletonBeanInvocation()) {
            throw new IllegalStateException("Creation of timers is not allowed during lifecycle callback of non-singleton EJBs");
        }
        if (schedule == null) {
            throw new IllegalArgumentException("schedule is null");
        }
        // parse the passed schedule and create the calendar based timeout
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);
        // generate a id for the timer
        UUID uuid = UUID.randomUUID();
        // create the timer
        TimerImpl timer = new CalendarTimer(uuid.toString(), this, calendarTimeout, info, persistent, timeoutMethod);

        if (persistent) {
            this.persistTimer(timer);
        }

        // now "start" the timer. This involves, moving the timer to an ACTIVE state
        // and scheduling the timer task
        this.startTimer(timer);

        this.addTimer(timer);
        // return the timer
        return timer;
    }

    /**
     * TODO: Rethink about this method. Do we really need this?
     * Adds the timer instance to an internal {@link javax.ejb.TimerHandle} to {@link TimerImpl} map.
     *
     * @param timer Timer instance
     */
    protected void addTimer(TimerImpl timer) {
        if (!timer.persistent) {
            nonPersistentTimers.put(timer.getTimerHandle(), timer);
        } else {
            this.persistentWaitingOnTxCompletionTimers.put(timer.getTimerHandle(), timer);
        }
    }

    /**
     * Returns the {@link TimedObjectInvoker} to which this timer service belongs
     *
     * @return
     */
    public TimedObjectInvoker getInvoker() {
        return invoker;
    }

    /**
     * Returns the {@link javax.ejb.Timer} corresponding to the passed {@link javax.ejb.TimerHandle}
     *
     * @param handle The {@link javax.ejb.TimerHandle} for which the {@link javax.ejb.Timer} is being looked for
     */
    public org.jboss.as.ejb3.timerservice.api.Timer getTimer(TimerHandle handle) {
        TimerImpl timer = nonPersistentTimers.get(handle);
        if (timer != null) {
            return timer;
        }
        timer = this.persistentWaitingOnTxCompletionTimers.get(handle);
        if (timer != null) {
            return timer;
        }
        TimerHandleImpl timerHandle = (TimerHandleImpl) handle;
        return this.getPersistedTimer(timerHandle);

    }

    /**
     * @return Returns the current transaction, if any. Else returns null.
     * @throws javax.ejb.EJBException If there is any system level exception
     */
    protected Transaction getTransaction() {
        try {
            return transactionManager.getTransaction();
        } catch (SystemException e) {
            throw new EJBException(e);
        }
    }

    /**
     * Remove a txtimer from the list of active timers
     */
    void removeTimer(TimerImpl timer) {
        if (!timer.persistent) {
            nonPersistentTimers.remove(timer.getTimerHandle());
        } else {
            this.persistentWaitingOnTxCompletionTimers.remove(timer.getTimerHandle());
            timerPersistence.removeTimer(timer.getPersistentState());
        }
    }

    /**
     * Persists the passed <code>timer</code>.
     * <p/>
     * <p>
     * If the passed timer is null or is non-persistent (i.e. {@link javax.ejb.Timer#isPersistent()} returns false),
     * then this method acts as a no-op
     * </p>
     *
     * @param timer
     */
    public void persistTimer(TimerImpl timer) {
        // if not persistent, then do nothing
        if (timer == null || !timer.persistent) {
            return;
        }

        // get the persistent entity from the timer
        final TimerEntity timerEntity = timer.getPersistentState();
        try {
            //if timer persistence is disabled
            if (timerPersistence == null) {
                logger.warn("Timer persistence is not enabled, persistent timers will not survive JVM restarts");
                return;
            }
            if (timerEntity.getTimerState() == TimerState.EXPIRED ||
                    timerEntity.getTimerState() == TimerState.CANCELED) {
                timerPersistence.removeTimer(timerEntity);
            } else {
                timerPersistence.persistTimer(timerEntity);
            }


        } catch (Throwable t) {
            this.setRollbackOnly();
            throw new RuntimeException(t);
        }
    }

    /**
     * Suspends any currently scheduled tasks for {@link javax.ejb.Timer}s
     * <p>
     * Note that, suspend does <b>not</b> cancel the {@link javax.ejb.Timer}. Instead,
     * it just cancels the <b>next scheduled timeout</b>. So once the {@link javax.ejb.Timer}
     * is restored (whenever that happens), the {@link javax.ejb.Timer} will continue to
     * timeout at appropriate times.
     * </p>
     */
    public void suspendTimers() {
        // get all active timers (persistent/non-persistent inclusive)
        Collection<Timer> timers = this.getTimers();
        for (Timer timer : timers) {
            if (!(timer instanceof TimerImpl)) {
                continue;
            }
            // suspend the timer
            ((TimerImpl) timer).suspend();
        }
    }

    /**
     * Restores persisted timers, corresponding to this timerservice, which are eligible for any new timeouts.
     * <p>
     * This includes timers whose {@link TimerState} is <b>neither</b> of the following:
     * <ul>
     * <li>{@link TimerState#CANCELED}</li>
     * <li>{@link TimerState#EXPIRED}</li>
     * </ul>
     * </p>
     * <p>
     * All such restored timers will be schedule for their next timeouts.
     * </p>
     *
     * @param autoTimers
     */
    public void restoreTimers(final List<ScheduleTimer> autoTimers) {
        // get the persisted timers which are considered active
        List<TimerImpl> restorableTimers = this.getActiveTimers();

        //timers are removed from the list as they are loaded
        final List<ScheduleTimer> newAutoTimers = new LinkedList<ScheduleTimer>(autoTimers);

        logger.debug("Found " + restorableTimers.size() + " active timers for timedObjectId: "
                + this.invoker.getTimedObjectId());
        // now "start" each of the restorable timer. This involves, moving the timer to an ACTIVE state
        // and scheduling the timer task
        for (final TimerImpl activeTimer : restorableTimers) {

            if (activeTimer.isAutoTimer()) {
                boolean found = false;
                final CalendarTimerEntity entity = (CalendarTimerEntity) activeTimer.getPersistentState();
                //so we know we have an auto timer. We need to try and match it up with the auto timers.
                ListIterator<ScheduleTimer> it = newAutoTimers.listIterator();
                while (it.hasNext()) {
                    ScheduleTimer timer = it.next();
                    final String methodName = timer.getMethod().getName();
                    final String[] params = new String[timer.getMethod().getParameterTypes().length];
                    for (int i = 0; i < timer.getMethod().getParameterTypes().length; ++i) {
                        params[i] = timer.getMethod().getParameterTypes()[i].getName();
                    }
                    if (doesTimeoutMethodMatch(entity.getTimeoutMethod(), methodName, params)) {

                        //the timers have the same method.
                        //now lets make sure the schedule is the same
                        if (this.doesScheduleMatch(entity.getScheduleExpression(), timer.getScheduleExpression())) {
                            it.remove();
                            found = true;
                            break;
                        }
                    }
                }
                if (found) {
                    startTimer(activeTimer);
                    logger.debug("Started timer: " + activeTimer);
                    // save any changes to the state (that will have happened on call to startTimer)
                    this.persistTimer(activeTimer);
                } else {
                    //the annotation is no longer there
                    this.removeTimer(activeTimer);
                }
            }
            //TODO: we need to make sure that these only fire one event after being restored
            this.startTimer(activeTimer);
            logger.debug("Started timer: " + activeTimer);
            // save any changes to the state (that will have happened on call to startTimer)
            this.persistTimer(activeTimer);
        }

        for (ScheduleTimer timer : newAutoTimers) {
            this.loadAutoTimer(timer.getScheduleExpression(), timer.getTimerConfig(), timer.getMethod());
        }

    }

    /**
     * Registers a timer with a transaction (if any in progress) and then moves
     * the timer to a active state, so that it becomes eligible for timeouts
     */
    protected void startTimer(TimerImpl timer) {
        registerTimerWithTx(timer);

        // the timer will actually go ACTIVE on tx commit
        startInTx(timer);
    }

    /**
     * Registers the timer with any active transaction so that appropriate action on the timer can be
     * carried out on transaction lifecycle events, through the use of {@link javax.transaction.Synchronization}
     * callbacks.
     * <p>
     * If there is no transaction in progress, when this method is called, then
     * this method is effectively a no-op.
     * </p>
     *
     * @param timer
     */
    protected void registerTimerWithTx(TimerImpl timer) {
        // get the current transaction
        Transaction tx = this.getTransaction();
        if (tx != null) {
            try {
                // register for lifecycle events of transaction
                tx.registerSynchronization(new TimerCreationTransactionSynchronization(timer));
            } catch (RollbackException e) {
                // TODO: throw the right exception
                throw new EJBException(e);
            } catch (SystemException e) {
                throw new EJBException(e);
            }
        }
    }

    /**
     * Moves the timer to either {@link TimerState#STARTED_IN_TX} or {@link TimerState#ACTIVE}
     * depending on whether there's any transaction active currently.
     * <p>
     * If there's no transaction currently active, then this method creates and schedules a timer task.
     * Else, it just changes the state of the timer to {@link TimerState#STARTED_IN_TX} and waits
     * for the transaction to commit, to schedule the timer task.
     * </p>
     *
     * @param timer
     */
    protected void startInTx(TimerImpl timer) {
        timer.setTimerState(TimerState.ACTIVE);
        this.persistTimer(timer);

        // if there's no transaction, then trigger a schedule immidiately.
        // Else, the timer will be scheduled on tx synchronization callback
        if (this.getTransaction() == null) {
            // create and schedule a timer task
            timer.scheduleTimeout();
        }

    }

    /**
     * Returns true if the {@link CurrentInvocationContext} represents a lifecycle
     * callback invocation. Else returns false.
     * <p>
     * This method internally relies on {@link CurrentInvocationContext#get()} to obtain
     * the current invocation context.
     * <ul>
     * <li>If the context is available then it looks for the method that was invoked.
     * The absence of a method indicates a lifecycle callback.</li>
     * <li>If the context is <i>not</i> available, then this method returns false (i.e.
     * it doesn't consider the current invocation as a lifecycle callback). This is
     * for convenience, to allow the invocation of {@link javax.ejb.TimerService} methods
     * in the absence of {@link CurrentInvocationContext}</li>
     * </ul>
     * <p/>
     * </p>
     *
     * @return
     */
    protected boolean isLifecycleCallbackInvocation() {
        InterceptorContext currentInvocationContext = null;
        try {
            currentInvocationContext = CurrentInvocationContext.get();
        } catch (IllegalStateException ise) {
            // no context info available so return false
            return false;
        }
        // If the method in current invocation context is null,
        // then it represents a lifecycle callback invocation
        Method invokedMethod = currentInvocationContext.getMethod();
        if (invokedMethod == null) {
            // it's a lifecycle callback
            return true;
        }
        // not an lifecycle callback
        return false;
    }

    /**
     * Creates and schedules a {@link TimerTask} for the next timeout of the passed <code>timer</code>
     */
    protected void scheduleTimeout(TimerImpl timer) {
        Date nextExpiration = timer.getNextExpiration();
        if (nextExpiration == null) {
            logger.info("Next expiration is null. No tasks will be scheduled for timer " + timer);
            return;
        }
        // create the timer task
        final Runnable timerTask = timer.getTimerTask();
        // find out how long is it away from now
        long delay = nextExpiration.getTime() - System.currentTimeMillis();
        // if in past, then trigger immediately
        if (delay < 0) {
            delay = 0;
        }
        long intervalDuration = timer.getInterval();
        if (intervalDuration > 0) {
            logger.debug("Scheduling timer " + timer + " at fixed rate, starting at " + delay
                    + " milli seconds from now with repeated interval=" + intervalDuration);
            // schedule the task
            final Task task = new Task(timerTask);
            this.timer.scheduleAtFixedRate(task, delay, intervalDuration);
            // maintain it in timerservice for future use (like cancellation)
            this.scheduledTimerFutures.put(timer.getTimerHandle(), task);
        } else {
            logger.debug("Scheduling a single action timer " + timer + " starting at " + delay + " milli seconds from now");
            // schedule the task
            this.timer.schedule(new Task(timerTask), delay);
            // maintain it in timerservice for future use (like cancellation)
            final Task task = new Task(timerTask);
            this.scheduledTimerFutures.put(timer.getTimerHandle(), task);
        }
    }

    /**
     * Cancels any scheduled {@link java.util.concurrent.Future} corresponding to the passed <code>timer</code>
     *
     * @param timer
     */
    protected void cancelTimeout(TimerImpl timer) {
        TimerHandle handle = timer.getTimerHandle();
        java.util.TimerTask timerTask = this.scheduledTimerFutures.get(handle);
        if (timerTask != null) {
            timerTask.cancel();
        }

    }

    private boolean isSingletonBeanInvocation() {
        return component instanceof SingletonComponent;
    }

    private TimerImpl getPersistedTimer(TimerHandleImpl timerHandle) {
        String id = timerHandle.getId();
        String timedObjectId = timerHandle.getTimedObjectId();
        if (timerPersistence == null) {
            return null;
        }

        TimerEntity timerEntity = timerPersistence.loadTimer(id, timedObjectId);
        if (timerEntity.isCalendarTimer()) {
            return new CalendarTimer((CalendarTimerEntity) timerEntity, this);
        }
        return new TimerImpl(timerEntity, this);

    }

    private List<TimerImpl> getActiveTimers() {
        // we need only those timers which correspond to the
        // timed object invoker to which this timer service belongs. So
        // first get hold of the timed object id
        final String timedObjectId = this.getInvoker().getTimedObjectId();

        // timer states which do *not* represent an active timer
        final Set<TimerState> ineligibleTimerStates = new HashSet<TimerState>();
        ineligibleTimerStates.add(TimerState.CANCELED);
        ineligibleTimerStates.add(TimerState.EXPIRED);
        if (timerPersistence == null) {
            //if the em is null then there are no persistent timers
            return Collections.emptyList();
        }


        final List<TimerEntity> persistedTimers = timerPersistence.loadActiveTimers(timedObjectId);
        final List<TimerImpl> activeTimers = new ArrayList<TimerImpl>();
        for (TimerEntity persistedTimer : persistedTimers) {
            if (ineligibleTimerStates.contains(persistedTimer.getTimerState())) {
                continue;
            }
            TimerImpl activeTimer = null;
            if (persistedTimer.isCalendarTimer()) {
                CalendarTimerEntity calendarTimerEntity = (CalendarTimerEntity) persistedTimer;

                // create a timer instance from the persisted calendar timer
                activeTimer = new CalendarTimer(calendarTimerEntity, this);
            } else {
                // create the timer instance from the persisted state
                activeTimer = new TimerImpl(persistedTimer, this);
            }
            // add it to the list of timers which will be restored
            activeTimers.add(activeTimer);
        }

        return activeTimers;
    }

    private Serializable clone(Serializable info) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(info);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        Object clonedInfo = objectInputStream.readObject();

        return (Serializable) clonedInfo;
    }

    private boolean doesTimeoutMethodMatch(TimeoutMethod timeoutMethod, String timeoutMethodName, String[] methodParams) {
        if (timeoutMethod.getMethodName().equals(timeoutMethodName) == false) {
            return false;
        }
        String[] timeoutMethodParams = timeoutMethod.getMethodParams();
        if (timeoutMethodParams == null && methodParams == null) {
            return true;
        }
        return this.methodParamsMatch(timeoutMethodParams, methodParams);
    }

    private boolean doesScheduleMatch(final ScheduleExpression expression1, final ScheduleExpression expression2) {
        if (!same(expression1.getDayOfMonth(), expression2.getDayOfMonth())) {
            return false;
        }
        if (!same(expression1.getDayOfWeek(), expression2.getDayOfWeek())) {
            return false;
        }
        if (!same(expression1.getEnd(), expression2.getEnd())) {
            return false;
        }
        if (!same(expression1.getHour(), expression2.getHour())) {
            return false;
        }
        if (!same(expression1.getMinute(), expression2.getMinute())) {
            return false;
        }
        if (!same(expression1.getMonth(), expression2.getMonth())) {
            return false;
        }
        if (!same(expression1.getSecond(), expression2.getSecond())) {
            return false;
        }
        if (!same(expression1.getStart(), expression2.getStart())) {
            return false;
        }
        if (!same(expression1.getTimezone(), expression2.getTimezone())) {
            return false;
        }
        if (!same(expression1.getYear(), expression2.getYear())) {
            return false;
        }
        return true;
    }

    private boolean same(Object i1, Object i2) {
        if (i1 == null && i2 != null) {
            return false;
        }
        if (i2 == null && i1 != null) {
            return false;
        }
        if (i1 == null && i2 == null) {
            return true;
        }
        return i1.equals(i2);
    }

    private boolean isEitherParamNull(Object param1, Object param2) {
        if (param1 != null && param2 == null) {
            return true;
        }
        if (param2 != null && param1 == null) {
            return true;
        }
        return false;
    }

    private boolean methodParamsMatch(String[] methodParams, String[] otherMethodParams) {
        if (this.isEitherParamNull(methodParams, otherMethodParams)) {
            return false;
        }

        if (methodParams.length != otherMethodParams.length) {
            return false;
        }
        for (int i = 0; i < methodParams.length; i++) {
            if (!methodParams[i].equals(otherMethodParams[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Marks the transaction for rollback
     * NOTE: This method will soon be removed, once this timer service
     * implementation becomes "managed"
     */
    private void setRollbackOnly() {
        try {
            Transaction tx = this.transactionManager.getTransaction();
            if (tx != null) {
                tx.setRollbackOnly();
            }
        } catch (IllegalStateException ise) {
            logger.error("Ignoring exception during setRollbackOnly: ", ise);
        } catch (SystemException se) {
            logger.error("Ignoring exception during setRollbackOnly: ", se);
        }
    }

    private void startNewTx() {
        try {
            this.transactionManager.begin();
        } catch (Throwable t) {
            throw new RuntimeException("Could not start transaction", t);
        }
    }

    private void endTx() {
        try {
            Transaction tx = this.transactionManager.getTransaction();
            if (tx == null) {
                throw new IllegalStateException("Transaction cannot be ended since no transaction is in progress");
            }
            if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                this.transactionManager.rollback();
            } else if (tx.getStatus() == Status.STATUS_ACTIVE) {
                // Commit tx
                this.transactionManager.commit();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not end transaction", e);
        }
    }


    private void handleLifecycleCallback() {
        if(isLifecycleCallbackInvocation() && !this.isSingletonBeanInvocation()) {
            throw new IllegalStateException("Cannot invoke timer service methods in lifecycle callback of non-singleton beans");
        }
    }

    private class TimerCreationTransactionSynchronization implements Synchronization {
        /**
         * The timer being managed in the transaction
         */
        private TimerImpl timer;

        public TimerCreationTransactionSynchronization(TimerImpl timer) {
            if (timer == null) {
                throw new IllegalStateException("Timer cannot be null");
            }
            this.timer = timer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void afterCompletion(int status) {
            if (this.timer.persistent) {
                synchronized (TimerServiceImpl.this.persistentWaitingOnTxCompletionTimers) {
                    TimerServiceImpl.this.persistentWaitingOnTxCompletionTimers.remove(this.timer.getTimerHandle());
                }
            }
            if (status == Status.STATUS_COMMITTED) {
                logger.debug("commit timer creation: " + this.timer);

                TimerState timerState = this.timer.getState();
                switch (timerState) {
                    case ACTIVE:
                        // the timer was started/activated in a tx.
                        // now it's time to schedule the task
                        this.timer.scheduleTimeout();
                        break;
                }
            } else if (status == Status.STATUS_ROLLEDBACK) {
                logger.debug("Rolling back timer creation: " + this.timer);

                TimerState timerState = this.timer.getState();
                switch (timerState) {
                    case ACTIVE:
                        if (this.timer.isPersistent()) {
                            timerPersistence.removeTimer(this.timer.getPersistentState());
                        }
                        this.timer.setTimerState(TimerState.CANCELED);
                        break;
                }

            }

        }

        @Override
        public void beforeCompletion() {

        }

    }

    private class Task extends java.util.TimerTask {

        private final Runnable delegate;

        public Task(final Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            executorService.submit(delegate);
        }
    }

}
