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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.as.ejb3.component.singleton.SingletonComponent;
import org.jboss.as.ejb3.component.stateful.CurrentSynchronizationCallback;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.ejb3.timerservice.persistence.CalendarTimerEntity;
import org.jboss.as.ejb3.timerservice.persistence.TimeoutMethod;
import org.jboss.as.ejb3.timerservice.persistence.TimerEntity;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.as.ejb3.timerservice.schedule.CalendarBasedTimeout;
import org.jboss.as.ejb3.timerservice.spi.ScheduleTimer;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.ejb3.timerservice.task.TimerTask;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * MK2 implementation of EJB3.1 {@link TimerService}
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class TimerServiceImpl implements TimerService, Service<TimerService> {

    /**
     * inactive timer states
     */
    private static final Set<TimerState> ineligibleTimerStates;

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(TimerServiceImpl.class);

    public static final ServiceName SERVICE_NAME = ServiceName.of("ejb3", "timerService");

    /**
     * The service name this timer service is registered under
     */
    private final ServiceName serviceName;

    private final InjectedValue<EJBComponent> ejbComponentInjectedValue = new InjectedValue<EJBComponent>();

    private final InjectedValue<ExecutorService> executorServiceInjectedValue = new InjectedValue<ExecutorService>();

    private final InjectedValue<java.util.Timer> timerInjectedValue = new InjectedValue<java.util.Timer>();

    private final InjectedValue<TimedObjectInvoker> timedObjectInvoker = new InjectedValue<TimedObjectInvoker>();

    /**
     * Auto timers that should be added on startup
     */
    private final Map<Method, List<AutoTimer>> autoTimers;

    /**
     * Used for persistent timers
     */
    private final InjectedValue<TimerPersistence> timerPersistence = new InjectedValue<TimerPersistence>();

    /**
     * All non-persistent timers which were created by this {@link TimerService}
     */
    private final Map<TimerHandle, TimerImpl> nonPersistentTimers = Collections.synchronizedMap(new HashMap<TimerHandle, TimerImpl>());

    /**
     * persistent timers that have been created in the current transaction
     */
    private final Map<TimerHandle, TimerImpl> persistentWaitingOnTxCompletionTimers = Collections.synchronizedMap(new HashMap<TimerHandle, TimerImpl>());

    /**
     * Holds the {@link java.util.concurrent.Future} of each of the timer tasks that have been scheduled
     */
    private final Map<TimerHandle, java.util.TimerTask> scheduledTimerFutures = Collections.synchronizedMap(new HashMap<TimerHandle, java.util.TimerTask>());

    private TransactionManager transactionManager;

    private volatile boolean started = false;

    static {
        final Set<TimerState> states = new HashSet<TimerState>();
        states.add(TimerState.CANCELED);
        states.add(TimerState.EXPIRED);
        ineligibleTimerStates = Collections.unmodifiableSet(states);
    }

    /**
     * Creates a {@link TimerServiceImpl}
     *
     * @param autoTimers
     * @param serviceName
     * @throws IllegalArgumentException If either of the passed param is null
     */
    public TimerServiceImpl(final Map<Method, List<AutoTimer>> autoTimers, final ServiceName serviceName) {
        this.autoTimers = autoTimers;
        this.serviceName = serviceName;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {

        logger.debug("Starting timerservice for timedObjectId: " + getInvoker().getTimedObjectId());
        final EJBComponent component = ejbComponentInjectedValue.getValue();
        this.transactionManager = component.getTransactionManager();
        final TimedObjectInvoker invoker = timedObjectInvoker.getValue();
        if (invoker == null) {
            throw MESSAGES.invokerIsNull();
        }
        final List<ScheduleTimer> timers = new ArrayList<ScheduleTimer>();

        for (Map.Entry<Method, List<AutoTimer>> entry : autoTimers.entrySet()) {
            for (AutoTimer timer : entry.getValue()) {
                timers.add(new ScheduleTimer(entry.getKey(), timer.getScheduleExpression(), timer.getTimerConfig()));
            }
        }
        // restore the timers
        restoreTimers(timers);
        started = true;
    }

    @Override
    public synchronized void stop(final StopContext context) {
        suspendTimers();
        timerPersistence.getValue().timerUndeployed(timedObjectInvoker.getValue().getTimedObjectId());
        started = false;
        this.transactionManager = null;
    }

    @Override
    public synchronized TimerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule) throws IllegalArgumentException,
            IllegalStateException, EJBException {
        return this.createCalendarTimer(schedule, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createCalendarTimer(ScheduleExpression schedule, TimerConfig timerConfig)
            throws IllegalArgumentException, IllegalStateException, EJBException {
        assertTimerServiceState();
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
        assertTimerServiceState();
        if (initialExpiration == null) {
            throw MESSAGES.initialExpirationIsNullCreatingTimer();
        }
        if (initialExpiration.getTime() < 0) {
            throw MESSAGES.invalidInitialExpiration("initialExpiration.getTime()");
        }
        if (intervalDuration < 0) {
            throw MESSAGES.invalidInitialExpiration("intervalDuration");
        }
        return this.createTimer(initialExpiration, intervalDuration, timerConfig.getInfo(), timerConfig.isPersistent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createIntervalTimer(long initialDuration, long intervalDuration, TimerConfig timerConfig)
            throws IllegalArgumentException, IllegalStateException, EJBException {
        assertTimerServiceState();
        if (initialDuration < 0) {
            throw MESSAGES.invalidInitialExpiration("intervalDuration");
        }
        if (intervalDuration < 0) {
            throw MESSAGES.invalidInitialExpiration("intervalDuration");
        }

        return this.createIntervalTimer(new Date(System.currentTimeMillis() + initialDuration), intervalDuration, timerConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createSingleActionTimer(Date expiration, TimerConfig timerConfig) throws IllegalArgumentException,
            IllegalStateException, EJBException {
        assertTimerServiceState();
        if (expiration == null) {
            throw MESSAGES.expirationIsNull();
        }
        if (expiration.getTime() < 0) {
            throw MESSAGES.invalidExpirationActionTimer();
        }
        return this.createTimer(expiration, 0, timerConfig.getInfo(), timerConfig.isPersistent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createSingleActionTimer(long duration, TimerConfig timerConfig) throws IllegalArgumentException,
            IllegalStateException, EJBException {
        assertTimerServiceState();
        if (duration < 0)
            throw MESSAGES.invalidDurationActionTimer();

        return createTimer(new Date(System.currentTimeMillis() + duration), 0, timerConfig.getInfo(), timerConfig
                .isPersistent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(long duration, Serializable info) throws IllegalArgumentException, IllegalStateException,
            EJBException {
        assertTimerServiceState();
        if (duration < 0)
            throw MESSAGES.invalidDurationTimer();
        return createTimer(new Date(System.currentTimeMillis() + duration), 0, info, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(Date expiration, Serializable info) throws IllegalArgumentException, IllegalStateException,
            EJBException {
        assertTimerServiceState();
        if (expiration == null) {
            throw MESSAGES.expirationDateIsNull();
        }
        if (expiration.getTime() < 0) {
            throw MESSAGES.invalidExpirationTimer();
        }
        return this.createTimer(expiration, 0, info, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(long initialDuration, long intervalDuration, Serializable info)
            throws IllegalArgumentException, IllegalStateException, EJBException {
        assertTimerServiceState();
        if (initialDuration < 0) {
            throw MESSAGES.invalidInitialDurationTimer();
        }
        if (intervalDuration < 0) {
            throw MESSAGES.invalidIntervalTimer();
        }
        return this.createTimer(new Date(System.currentTimeMillis() + initialDuration), intervalDuration, info, true);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createTimer(Date initialExpiration, long intervalDuration, Serializable info)
            throws IllegalArgumentException, IllegalStateException, EJBException {
        assertTimerServiceState();
        if (initialExpiration == null) {
            throw MESSAGES.initialExpirationDateIsNull();
        }
        if (initialExpiration.getTime() < 0) {
            throw MESSAGES.invalidExpirationTimer();
        }
        if (intervalDuration < 0) {
            throw MESSAGES.invalidIntervalDurationTimer();
        }
        return this.createTimer(initialExpiration, intervalDuration, info, true);
    }

    public TimerImpl loadAutoTimer(ScheduleExpression schedule,
                                   TimerConfig timerConfig, Method timeoutMethod) {
        return this.createCalendarTimer(schedule, timerConfig.getInfo(), timerConfig.isPersistent(), timeoutMethod);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Timer> getTimers() throws IllegalStateException, EJBException {
        assertTimerServiceState();
        Object pk = currentPrimaryKey();
        final Set<Timer> activeTimers = new HashSet<Timer>();
        // get all active non-persistent timers for this timerservice
        for (final TimerImpl timer : this.nonPersistentTimers.values()) {
            if (ineligibleTimerStates.contains(timer.getState())) {
                continue;
            } else if (timer.isActive()) {
                if (timer.getPrimaryKey() == null || timer.getPrimaryKey().equals(pk)) {
                    activeTimers.add(timer);
                }
            }
        }
        // get all active timers which are persistent, but haven't yet been
        // persisted (waiting for tx to complete)
        for (final TimerImpl timer : this.persistentWaitingOnTxCompletionTimers.values()) {
            if (ineligibleTimerStates.contains(timer.getState())) {
                continue;
            } else if (timer.isActive()) {
                if (timer.getPrimaryKey() == null || timer.getPrimaryKey().equals(pk)) {
                    activeTimers.add(timer);
                }
            }
        }

        // now get all active persistent timers for this timerservice
        for (final TimerImpl timer : this.getActivePersistentTimers(pk)) {
            activeTimers.add(timer);
        }
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
            throw MESSAGES.failToCreateTimerDoLifecycle();
        }
        if (initialExpiration == null) {
            throw MESSAGES.initialExpirationIsNull();
        }
        if (intervalDuration < 0) {
            throw MESSAGES.invalidIntervalDuration();
        }

        // create an id for the new timer instance
        UUID uuid = UUID.randomUUID();
        // create the timer

        TimerImpl timer = new TimerImpl(uuid.toString(), this, initialExpiration, intervalDuration, info, persistent, currentPrimaryKey(), TimerState.CREATED);
        // now "start" the timer. This involves, moving the timer to an ACTIVE state
        // and scheduling the timer task
        this.startTimer(timer);

        this.addTimer(timer);
        this.persistTimer(timer);
        // return the newly created timer
        return timer;
    }

    /**
     * @return The primary key of the current EJB, or null if not applicable
     */
    private Object currentPrimaryKey() {

        final InterceptorContext context = CurrentInvocationContext.get();

        if (context == null) {
            return null;
        }
        final ComponentInstance instance = context.getPrivateData(ComponentInstance.class);
        if (instance instanceof EntityBeanComponentInstance) {
            return ((EntityBeanComponentInstance) instance).getPrimaryKey();
        }
        return null;
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
    private TimerImpl createCalendarTimer(ScheduleExpression schedule,
                                          Serializable info, boolean persistent, Method timeoutMethod) {
        if (this.isLifecycleCallbackInvocation() && !this.isSingletonBeanInvocation()) {
            throw MESSAGES.failToCreateTimerDoLifecycle();
        }
        if (schedule == null) {
            throw MESSAGES.scheduleIsNull();
        }
        // parse the passed schedule and create the calendar based timeout
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);
        // generate a id for the timer
        UUID uuid = UUID.randomUUID();
        // create the timer
        TimerImpl timer = new CalendarTimer(uuid.toString(), this, calendarTimeout, info, persistent, timeoutMethod, currentPrimaryKey());

        // now "start" the timer. This involves, moving the timer to an ACTIVE state
        // and scheduling the timer task
        this.startTimer(timer);

        this.addTimer(timer);
        this.persistTimer(timer);
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
        return timedObjectInvoker.getValue();
    }

    /**
     * Returns the {@link javax.ejb.Timer} corresponding to the passed {@link javax.ejb.TimerHandle}
     *
     * @param handle The {@link javax.ejb.TimerHandle} for which the {@link javax.ejb.Timer} is being looked for
     */
    public TimerImpl getTimer(TimerHandle handle) {
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
     * Persists the passed <code>timer</code>.
     * <p/>
     * <p>
     * If the passed timer is null or is non-persistent (i.e. {@link javax.ejb.Timer#isPersistent()} returns false),
     * then this method acts as a no-op
     * </p>
     *
     * @param timer
     */
    public void persistTimer(final TimerImpl timer) {
        if (timer == null) {
            return;
        }
        if (!timer.persistent) {
            switch (timer.getState()) {
                case EXPIRED:
                    nonPersistentTimers.remove(timer.handle);
                    break;
                case CANCELED:
                    //we only want to remove it on TX end
                    if (transactionActive()) {
                        registerSynchronization(new NonPersistentTimerRemoveSynchronization(timer.handle));
                    } else {
                        nonPersistentTimers.remove(timer.handle);
                    }
            }
        } else {
            // get the persistent entity from the timer
            final TimerEntity timerEntity = timer.getPersistentState();
            try {
                if (timerPersistence == null) {
                    ROOT_LOGGER.timerPersistenceNotEnable();
                    return;
                }
                timerPersistence.getValue().persistTimer(timerEntity);

            } catch (Throwable t) {
                this.setRollbackOnly();
                throw new RuntimeException(t);
            }
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
        List<TimerImpl> restorableTimers = this.getActivePersistentTimers(null);

        //timers are removed from the list as they are loaded
        final List<ScheduleTimer> newAutoTimers = new LinkedList<ScheduleTimer>(autoTimers);

        ROOT_LOGGER.debug("Found " + restorableTimers.size() + " active timers for timedObjectId: "
                + getInvoker().getTimedObjectId());
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
                if (!found) {
                    activeTimer.setTimerState(TimerState.CANCELED);
                } else {
                    startTimer(activeTimer);
                    ROOT_LOGGER.debug("Started timer: " + activeTimer);
                }
                this.persistTimer(activeTimer);
            } else if (!ineligibleTimerStates.contains(activeTimer.getState())) {
                this.startTimer(activeTimer);
            }
            ROOT_LOGGER.debug("Started timer: " + activeTimer);
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


        // if there's no transaction, then trigger a schedule immidiately.
        // Else, the timer will be scheduled on tx synchronization callback
        if (!transactionActive()) {
            timer.setTimerState(TimerState.ACTIVE);
            // create and schedule a timer task
            timer.scheduleTimeout();
        } else {
            registerTimerWithTx(timer);
        }
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
        registerSynchronization(new TimerCreationTransactionSynchronization(timer));
    }

    private void registerSynchronization(Synchronization synchronization) {
        try {
            final Transaction tx = this.getTransaction();
            // register for lifecycle events of transaction
            tx.registerSynchronization(synchronization);
        } catch (RollbackException e) {
            throw new EJBException(e);
        } catch (SystemException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @return true if the transaction is in a state where synchronizations can be registered
     */
    boolean transactionActive() {
        final Transaction currentTx = getTransaction();
        if (currentTx != null) {
            try {
                int status = currentTx.getStatus();
                if (status == Status.STATUS_MARKED_ROLLBACK || status == Status.STATUS_ROLLEDBACK ||
                        status == Status.STATUS_ROLLING_BACK || status == Status.STATUS_NO_TRANSACTION ||
                        status == Status.STATUS_UNKNOWN || status == Status.STATUS_COMMITTED
                        || isBeforeCompletion()) {
                    return false;
                } else {
                    return true;
                }
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
        } else {
            return false;
        }
    }

    private boolean isBeforeCompletion() {
        final CurrentSynchronizationCallback.CallbackType type = CurrentSynchronizationCallback.get();
        if (type != null) {
            return type == CurrentSynchronizationCallback.CallbackType.BEFORE_COMPLETION;
        }
        return false;
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
        final InterceptorContext currentInvocationContext = CurrentInvocationContext.get();
        if (currentInvocationContext == null) {
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
            ROOT_LOGGER.nextExpirationIsNull(timer);
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
        final Task task = new Task(timerTask);
        if (intervalDuration > 0) {
            ROOT_LOGGER.debug("Scheduling timer " + timer + " at fixed rate, starting at " + delay
                    + " milli seconds from now with repeated interval=" + intervalDuration);
            // schedule the task
            this.timerInjectedValue.getValue().scheduleAtFixedRate(task, delay, intervalDuration);
            // maintain it in timerservice for future use (like cancellation)
            this.scheduledTimerFutures.put(timer.getTimerHandle(), task);
        } else {
            ROOT_LOGGER.debug("Scheduling a single action timer " + timer + " starting at " + delay + " milli seconds from now");
            // schedule the task
            this.timerInjectedValue.getValue().schedule(task, delay);
            // maintain it in timerservice for future use (like cancellation)
            this.scheduledTimerFutures.put(timer.getTimerHandle(), task);

        }
    }

    /**
     * Cancels any scheduled {@link java.util.concurrent.Future} corresponding to the passed <code>timer</code>
     *
     * @param timer
     */
    protected void cancelTimeout(final TimerImpl timer) {
        TimerHandle handle = timer.getTimerHandle();
        java.util.TimerTask timerTask = this.scheduledTimerFutures.remove(handle);
        if (timerTask != null) {
            timerTask.cancel();
        }

    }

    private boolean isSingletonBeanInvocation() {
        return ejbComponentInjectedValue.getValue() instanceof SingletonComponent;
    }

    private TimerImpl getPersistedTimer(TimerHandleImpl timerHandle) {
        String id = timerHandle.getId();
        String timedObjectId = timerHandle.getTimedObjectId();
        if (timerPersistence == null) {
            return null;
        }

        final TimerEntity timerEntity = timerPersistence.getValue().loadTimer(id, timedObjectId);
        if (timerEntity == null) {
            throw new NoSuchObjectLocalException("Could not load timer with id " + id);
        }
        if (timerEntity.isCalendarTimer()) {
            return new CalendarTimer((CalendarTimerEntity) timerEntity, this);
        }
        return new TimerImpl(timerEntity, this);

    }

    private List<TimerImpl> getActivePersistentTimers(final Object primaryKey) {
        // we need only those timers which correspond to the
        // timed object invoker to which this timer service belongs. So
        // first get hold of the timed object id
        final String timedObjectId = this.getInvoker().getTimedObjectId();

        // timer states which do *not* represent an active timer

        if (timerPersistence == null) {
            //if the em is null then there are no persistent timers
            return Collections.emptyList();
        }


        final List<TimerEntity> persistedTimers;
        if(primaryKey == null) {
            persistedTimers = timerPersistence.getValue().loadActiveTimers(timedObjectId);
        } else {
            persistedTimers = timerPersistence.getValue().loadActiveTimers(timedObjectId, primaryKey);
        }
        final List<TimerImpl> activeTimers = new ArrayList<TimerImpl>();
        for (final TimerEntity persistedTimer : persistedTimers) {
            if (ineligibleTimerStates.contains(persistedTimer.getTimerState())) {
                continue;
            }
            TimerImpl activeTimer;
            if (persistedTimer.isCalendarTimer()) {
                final CalendarTimerEntity calendarTimerEntity = (CalendarTimerEntity) persistedTimer;
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

    private Serializable clone(final Serializable info) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(info);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        Object clonedInfo = objectInputStream.readObject();

        return (Serializable) clonedInfo;
    }

    private boolean doesTimeoutMethodMatch(final TimeoutMethod timeoutMethod, final String timeoutMethodName, final String[] methodParams) {
        if (timeoutMethod.getMethodName().equals(timeoutMethodName) == false) {
            return false;
        }
        final String[] timeoutMethodParams = timeoutMethod.getMethodParams();
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
            ROOT_LOGGER.ignoringException(ise);
        } catch (SystemException se) {
            ROOT_LOGGER.ignoringException(se);
        }
    }

    private void startNewTx() {
        try {
            this.transactionManager.begin();
        } catch (Throwable t) {
            throw MESSAGES.failToStartTransaction(t);
        }
    }

    private void assertTimerServiceState() {
        TimerServiceDisabledTacker.assertEnabled();
        if (isLifecycleCallbackInvocation() && !this.isSingletonBeanInvocation()) {
            throw MESSAGES.failToInvokeTimerServiceDoLifecycle();
        }
    }

    public InjectedValue<EJBComponent> getEjbComponentInjectedValue() {
        return ejbComponentInjectedValue;
    }

    public InjectedValue<ExecutorService> getExecutorServiceInjectedValue() {
        return executorServiceInjectedValue;
    }

    public InjectedValue<java.util.Timer> getTimerInjectedValue() {
        return timerInjectedValue;
    }

    public InjectedValue<TimerPersistence> getTimerPersistence() {
        return timerPersistence;
    }

    public ServiceName getServiceName() {
        return serviceName;
    }

    public boolean isStarted() {
        return started;
    }

    public InjectedValue<TimedObjectInvoker> getTimedObjectInvoker() {
        return timedObjectInvoker;
    }

    private class TimerCreationTransactionSynchronization implements Synchronization {
        /**
         * The timer being managed in the transaction
         */
        private TimerImpl timer;

        public TimerCreationTransactionSynchronization(TimerImpl timer) {
            if (timer == null) {
                throw MESSAGES.timerIsNull();
            }
            this.timer = timer;
        }

        @Override
        public void beforeCompletion() {
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
                ROOT_LOGGER.debug("commit timer creation: " + this.timer);

                TimerState timerState = this.timer.getState();
                switch (timerState) {
                    case CREATED:
                        this.timer.setTimerState(TimerState.ACTIVE);
                        this.timer.scheduleTimeout();
                        break;
                    case ACTIVE:
                        this.timer.scheduleTimeout();
                        break;
                }
            } else if (status == Status.STATUS_ROLLEDBACK) {
                ROOT_LOGGER.debug("Rolling back timer creation: " + this.timer);
                if (!timer.isPersistent()) {
                    nonPersistentTimers.remove(timer.handle);
                }
                TimerState timerState = this.timer.getState();
                switch (timerState) {
                    case ACTIVE:
                        this.timer.setTimerState(TimerState.CANCELED);
                        break;
                }

            }

        }
    }


    private class NonPersistentTimerRemoveSynchronization implements Synchronization {

        private final TimerHandle timerHandle;

        private NonPersistentTimerRemoveSynchronization(final TimerHandle timerHandle) {
            this.timerHandle = timerHandle;
        }

        @Override
        public void beforeCompletion() {

        }

        @Override
        public void afterCompletion(final int status) {
            if (status == Status.STATUS_COMMITTED) {
                nonPersistentTimers.remove(timerHandle);
            }
        }
    }

    private class Task extends java.util.TimerTask {

        private final Runnable delegate;

        public Task(final Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            final ExecutorService executor = executorServiceInjectedValue.getOptionalValue();
            if (executor != null) {
                executor.submit(delegate);
            }
        }
    }

}
