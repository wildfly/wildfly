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

import static org.jboss.as.ejb3.logging.EjbLogger.EJB3_TIMER_LOGGER;

import java.io.Closeable;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.TimerServiceRegistry;
import org.jboss.as.ejb3.component.allowedmethods.AllowedMethodsInformation;
import org.jboss.as.ejb3.component.allowedmethods.MethodType;
import org.jboss.as.ejb3.component.singleton.SingletonComponent;
import org.jboss.as.ejb3.component.stateful.CurrentSynchronizationCallback;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.subsystem.deployment.TimerServiceResource;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.as.ejb3.timerservice.persistence.database.DatabaseTimerPersistence;
import org.jboss.as.ejb3.timerservice.schedule.CalendarBasedTimeout;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.invocation.InterceptorContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * MK2 implementation of Enterprise Beans 3.1 {@link TimerService}
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class TimerServiceImpl implements TimerService, Service<TimerService> {
    /**
     * Flag to enable programmatic timer refresh from database timer persistence.
     * When set to true, {@link #getAllTimers()} method programmatically refreshes
     * from the database timer persistence before returning all timers.
     */
    private static final String PROGRAMMATIC_TIMER_REFRESH_ENABLED = "wildfly.ejb.timer.refresh.enabled";

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
     * All timers which were created by this {@link TimerService}
     */
    private final ConcurrentMap<String, TimerImpl> timers = new ConcurrentHashMap<>();

    /**
     * Holds the {@link java.util.concurrent.Future} of each of the timer tasks that have been scheduled
     */
    private final ConcurrentMap<String, java.util.TimerTask> scheduledTimerFutures = new ConcurrentHashMap<>();

    /**
     * Key that is used to store timers that are waiting on transaction completion in the transaction local
     */
    private final Object waitingOnTxCompletionKey = new Object();

    private TransactionSynchronizationRegistry tsr;
    private final TimerServiceRegistry timerServiceRegistry;

    /**
     * Dynamic resource. Exposed under service=timer-service.
     */
    private TimerServiceResource resource = new TimerServiceResource();

    private Closeable listenerHandle;

    private volatile boolean started = false;

    private static final Integer MAX_RETRY = Integer.getInteger("jboss.timer.TaskPostPersist.maxRetry", 10);

    /**
     * Creates a {@link TimerServiceImpl}
     *
     * @param autoTimers  The auto timers associated with this timer service
     * @param serviceName The service name of this timer service
     * @param registry    The {@link TimerServiceRegistry} which has the knowledge of other timer services belonging to the EJB module to which this
     *                    timer service belongs.
     */
    public TimerServiceImpl(final Map<Method, List<AutoTimer>> autoTimers, final ServiceName serviceName, final TimerServiceRegistry registry) {
        this.autoTimers = autoTimers;
        this.serviceName = serviceName;
        this.timerServiceRegistry = registry;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {

        if (EJB3_TIMER_LOGGER.isDebugEnabled()) {
            EJB3_TIMER_LOGGER.debug("Starting timerservice for timedObjectId: " + getInvoker().getTimedObjectId());
        }
        final EJBComponent component = ejbComponentInjectedValue.getValue();
        this.tsr = component.getTransactionSynchronizationRegistry();
        final TimedObjectInvoker invoker = timedObjectInvoker.getValue();
        if (invoker == null) {
            throw EJB3_TIMER_LOGGER.invokerIsNull();
        }

        started = true;
        timerPersistence.getValue().timerDeployed(timedObjectInvoker.getValue().getTimedObjectId());
        // register ourselves to the TimerServiceRegistry (if any)
        if (timerServiceRegistry != null) {
            timerServiceRegistry.registerTimerService(this);
        }
        listenerHandle = timerPersistence.getValue().registerChangeListener(getInvoker().getTimedObjectId(), new TimerRefreshListener());
    }

    @Override
    public synchronized void stop(final StopContext context) {
        // un-register ourselves to the TimerServiceRegistry (if any)
        if (timerServiceRegistry != null) {
            timerServiceRegistry.unRegisterTimerService(this);
        }

        timerPersistence.getValue().timerUndeployed(timedObjectInvoker.getValue().getTimedObjectId());
        started = false;
        safeClose(listenerHandle);
        listenerHandle = null;
        timerInjectedValue.getValue().purge(); //WFLY-3823
    }


    public synchronized void activate() {
        final List<AutoTimer> timers;
        if (autoTimers.isEmpty()) {
            timers = Collections.emptyList();
        } else {
            timers = new ArrayList<>();
            for (Map.Entry<Method, List<AutoTimer>> entry : autoTimers.entrySet()) {
                for (AutoTimer timer : entry.getValue()) {
                    timers.add(new AutoTimer(timer.getScheduleExpression(), timer.getTimerConfig(), entry.getKey()));
                }
            }
        }
        // restore the timers
        restoreTimers(timers);
    }

    public synchronized void deactivate() {
        suspendTimers();
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
        if (schedule == null) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("schedule", null);
        }

        final ScheduleExpression scheduleClone = new ScheduleExpression()
                .second(schedule.getSecond())
                .minute(schedule.getMinute())
                .hour(schedule.getHour())
                .dayOfMonth(schedule.getDayOfMonth())
                .dayOfWeek(schedule.getDayOfWeek())
                .month(schedule.getMonth())
                .year(schedule.getYear())
                .timezone(schedule.getTimezone())
                .start(schedule.getStart())
                .end(schedule.getEnd());
        Serializable info = timerConfig == null ? null : timerConfig.getInfo();
        boolean persistent = timerConfig == null || timerConfig.isPersistent();
        return this.createCalendarTimer(scheduleClone, info, persistent, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timer createIntervalTimer(Date initialExpiration, long intervalDuration, TimerConfig timerConfig)
            throws IllegalArgumentException, IllegalStateException, EJBException {
        assertTimerServiceState();
        if (initialExpiration == null) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("initialExpiration", null);
        }
        if (initialExpiration.getTime() < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("initialExpiration.getTime()", Long.toString(initialExpiration.getTime()));
        }
        if (intervalDuration < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("intervalDuration", Long.toString(intervalDuration));
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
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("initialDuration", Long.toString(initialDuration));
        }
        if (intervalDuration < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("intervalDuration", Long.toString(intervalDuration));
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
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("expiration date", null);
        }
        if (expiration.getTime() < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("expiration.getTime", Long.toString(expiration.getTime()));
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
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("duration", Long.toString(duration));

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
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("duration", Long.toString(duration));
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
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("expiration date", null);
        }
        if (expiration.getTime() < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("expiration.getTime", Long.toString(expiration.getTime()));
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
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("initialDuration", Long.toString(initialDuration));
        }
        if (intervalDuration < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("intervalDuration", Long.toString(intervalDuration));
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
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("initialExpiration", null);
        }
        if (initialExpiration.getTime() < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("initialExpiration.getTime", Long.toString(initialExpiration.getTime()));
        }
        if (intervalDuration < 0) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("intervalDuration", Long.toString(intervalDuration));
        }
        return this.createTimer(initialExpiration, intervalDuration, info, true);
    }

    public TimerImpl loadAutoTimer(ScheduleExpression schedule,
                                   TimerConfig timerConfig, Method timeoutMethod) {
        if (this.isLifecycleCallbackInvocation() && !this.isSingletonBeanInvocation()) {
            throw EJB3_TIMER_LOGGER.failToInvokeTimerServiceDoLifecycle();
        }
        if (schedule == null) {
            throw EJB3_TIMER_LOGGER.invalidTimerParameter("schedule", null);
        }
        return this.createCalendarTimer(schedule, timerConfig.getInfo(), timerConfig.isPersistent(), timeoutMethod);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Timer> getTimers() throws IllegalStateException, EJBException {
        assertTimerServiceState();
        // get all active timers for this timerservice
        final Collection<TimerImpl> values = this.timers.values();
        final List<Timer> activeTimers = new ArrayList<>(values.size() + 10);
        for (final TimerImpl timer : values) {
            // Less disruptive way to get WFLY-8457 fixed.
            if (timer.isActive() || timer.getState() == TimerState.ACTIVE) {
                activeTimers.add(timer);
            }
        }

        // get all active timers which are persistent, but haven't yet been
        // persisted (waiting for tx to complete) that are in the current transaction
        for (final TimerImpl timer : getWaitingOnTxCompletionTimers().values()) {
            if (timer.isActive()) {
                activeTimers.add(timer);
            }
        }
        return activeTimers;
    }

    /**
     * {@inheritDoc}
     * <p>
     * When {@link #PROGRAMMATIC_TIMER_REFRESH_ENABLED} is set to true,
     * this method programmatically refreshes from the database timer persistence
     * before returning all timers.
     */
    @Override
    public Collection<Timer> getAllTimers() throws IllegalStateException, EJBException {
        final TimerPersistence persistence = timerPersistence.getOptionalValue();
        if (persistence instanceof DatabaseTimerPersistence) {
            final InterceptorContext currentInvocationContext = CurrentInvocationContext.get();
            if (currentInvocationContext != null) {
                try {
                    final Map<String, Object> contextData = currentInvocationContext.getContextData();
                    final Object flag = contextData.get(PROGRAMMATIC_TIMER_REFRESH_ENABLED);
                    if (Boolean.TRUE.equals(flag) || "true".equals(flag)) {
                        ((DatabaseTimerPersistence) persistence).refreshTimers();
                    }
                } catch (IllegalStateException e) {
                    //ignore, context data is not set
                }
            }
        }

        // query the registry
        if (this.timerServiceRegistry != null) {
            return this.timerServiceRegistry.getAllActiveTimers();
        }
        // if we don't have the registry (shouldn't really happen) which stores the timer services applicable for the Jakarta Enterprise Beans module to which
        // this timer service belongs, then let's at least return the active timers that are applicable only for this timer service
        return this.getTimers();
    }

    /**
     * Create a {@link javax.ejb.Timer}. Caller of this method should already have checked for allowed operations,
     * and validated parameters.
     *
     * @param initialExpiration The {@link java.util.Date} at which the first timeout should occur.
     *                          <p>If the date is in the past, then the timeout is triggered immediately
     *                          when the timer moves to {@link TimerState#ACTIVE}</p>
     * @param intervalDuration  The interval (in milliseconds) between consecutive timeouts for the newly created timer.
     *                          <p>Cannot be a negative value. A value of 0 indicates a single timeout action</p>
     * @param info              {@link java.io.Serializable} info that will be made available through the newly created timer's {@link javax.ejb.Timer#getInfo()} method
     * @param persistent        True if the newly created timer has to be persistent
     * @return Returns the newly created timer
     */
    private Timer createTimer(Date initialExpiration, long intervalDuration, Serializable info, boolean persistent) {
        // allowed method check and parameter validation are already done in all code paths before reaching here.
        // create an id for the new timer instance
        UUID uuid = UUID.randomUUID();
        // create the timer

        TimerImpl timer = TimerImpl.builder()
                .setNewTimer(true)
                .setId(uuid.toString())
                .setInitialDate(initialExpiration)
                .setRepeatInterval(intervalDuration)
                .setInfo(info)
                .setPersistent(persistent)
                .setTimerState(TimerState.CREATED)
                .setTimedObjectId(getInvoker().getTimedObjectId())
                .build(this);

        // now "start" the timer. This involves, moving the timer to an ACTIVE state
        // and scheduling the timer task

        this.persistTimer(timer, true);
        this.startTimer(timer);
        // return the newly created timer
        return timer;
    }

    /**
     * Creates a calendar based {@link javax.ejb.Timer}. Caller of this method should
     * already have checked for allowed operations, and validated parameters.
     *
     * @param schedule   The {@link javax.ejb.ScheduleExpression} which will be used for creating scheduled timer tasks
     *                   for a calendar based timer
     * @param info       {@link java.io.Serializable} info that will be made available through the newly created timer's {@link javax.ejb.Timer#getInfo()} method
     * @param persistent True if the newly created timer has to be persistent
     * @return Returns the newly created timer
     */
    private TimerImpl createCalendarTimer(ScheduleExpression schedule,
                                          Serializable info, boolean persistent, Method timeoutMethod) {
        // allowed method check and parameter validation are already done in all code paths before reaching here.
        // generate an id for the timer
        UUID uuid = UUID.randomUUID();
        // create the timer
        TimerImpl timer = CalendarTimer.builder()
                .setAutoTimer(timeoutMethod != null)
                .setScheduleExpression(schedule)
                .setTimeoutMethod(timeoutMethod)
                .setTimerState(TimerState.CREATED)
                .setId(uuid.toString())
                .setPersistent(persistent)
                .setTimedObjectId(getInvoker().getTimedObjectId())
                .setInfo(info)
                .setNewTimer(true)
                .build(this);

        this.persistTimer(timer, true);
        // now "start" the timer. This involves, moving the timer to an ACTIVE state
        // and scheduling the timer task

        // If an auto timer has been persisted by another node, it will be marked as CANCELED
        // during the persistTimer(timer, true) call above. This timer will be created or started.
        if (timeoutMethod != null && timer.getState() == TimerState.CANCELED) {
            EJB3_TIMER_LOGGER.debugv("The auto timer was already created by other node: {0}", timer);
            return timer;
        }

        this.startTimer(timer);
        // return the timer
        return timer;
    }

    public TimerImpl getTimer(final String timerId) {
        return this.timers.get(timerId);
    }

    /**
     * Retrieves the timer info from the timer database.
     *
     * @param timer the timer whose info to be retrieved
     * @return the timer info from database; cached timer info if the timer persistence store is not database
     */
    public Serializable getPersistedTimerInfo(final TimerImpl timer) {
        final TimerPersistence persistence = this.timerPersistence.getOptionalValue();
        if (persistence instanceof DatabaseTimerPersistence) {
            final DatabaseTimerPersistence databasePersistence = (DatabaseTimerPersistence) persistence;
            return databasePersistence.getPersistedTimerInfo(timer);
        }
        return timer.getCachedTimerInfo();
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
     * Returns the timer corresponding to the passed timer id and timed object id.
     *
     * @param timerId timer id
     * @param timedObjectId timed object id
     * @return the {@code TimerImpl} corresponding to the passed timer id and timed object id
     */
    public TimerImpl getTimer(final String timerId, final String timedObjectId) {
        TimerImpl timer;
        timer = this.timers.get(timerId);
        if (timer != null) {
            return timer;
        }
        timer = getWaitingOnTxCompletionTimers().get(timerId);
        if (timer != null) {
            return timer;
        }
        final TimerPersistence persistence = timerPersistence.getOptionalValue();
        if (persistence instanceof DatabaseTimerPersistence) {
            timer = ((DatabaseTimerPersistence) persistence).loadTimer(
                    timedObjectId, timerId, this);
        }
        return timer;
    }

    /**
     * @return Returns the current transaction, if any. Else returns null.
     * @throws javax.ejb.EJBException If there is any system level exception
     */
    protected Transaction getTransaction() {
        return ContextTransactionManager.getInstance().getTransaction();
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
    public void persistTimer(final TimerImpl timer, boolean newTimer) {
        if (timer == null) {
            return;
        }
        if (timer.persistent) {
            try {
                if (timerPersistence.getOptionalValue() == null) {
                    EJB3_TIMER_LOGGER.timerPersistenceNotEnable();
                    return;
                }
                final ContextTransactionManager transactionManager = ContextTransactionManager.getInstance();
                Transaction clientTX = transactionManager.getTransaction();
                if (newTimer || timer.isCanceled()) {
                    if (clientTX == null) {
                        transactionManager.begin();
                    }
                    try {
                        if (newTimer) timerPersistence.getValue().addTimer(timer);
                        else timerPersistence.getValue().persistTimer(timer);
                        if (clientTX == null) transactionManager.commit();
                    } catch (Exception e) {
                        if (clientTX == null) {
                            try {
                                transactionManager.rollback();
                            } catch (Exception ee) {
                                EjbLogger.EJB3_TIMER_LOGGER.timerUpdateFailedAndRollbackNotPossible(ee);
                            }
                        }
                        throw e;
                    }
                } else {
                    new TaskPostPersist(timer).persistTimer();
                }

            } catch (Throwable t) {
                this.setRollbackOnly();
                throw new RuntimeException(t);
            }
        }
    }

    public void cancelTimer(final TimerImpl timer) throws InterruptedException {
        timer.lock();
        boolean release = true;
        try {
            timer.assertTimerState();
            // first check whether the timer has expired or has been cancelled
            if (timer.getState() != TimerState.EXPIRED) {
                timer.setTimerState(TimerState.CANCELED, null);
            }

            release = removeTimer(timer);

            // persist changes
            persistTimer(timer, false);
        } finally {
            if (release) {
                timer.unlock();
            }
        }
    }

    private boolean removeTimer(final TimerImpl timer) {
        boolean startedInTx = getWaitingOnTxCompletionTimers().containsKey(timer.getId());

        if (transactionActive() && !startedInTx) {
            registerSynchronization(new TimerRemoveSynchronization(timer));
            return false;
        } else {
            // cancel any scheduled Future for this timer
            this.cancelTimeout(timer);
            this.unregisterTimerResource(timer.getId());
            return true;
        }
    }

    public void expireTimer(final TimerImpl timer) {
        this.cancelTimeout(timer);
        timer.setTimerState(TimerState.EXPIRED, null);
        this.unregisterTimerResource(timer.getId());
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
     * @param newAutoTimers
     */
    public void restoreTimers(final List<AutoTimer> newAutoTimers) {
        // get the persisted timers which are considered active
        List<TimerImpl> restorableTimers = this.getActivePersistentTimers();

        if (EJB3_TIMER_LOGGER.isDebugEnabled()) {
            EJB3_TIMER_LOGGER.debug("Found " + restorableTimers.size() + " active persistentTimers for timedObjectId: "
                    + getInvoker().getTimedObjectId());
        }
        // now "start" each of the restorable timer. This involves, moving the timer to an ACTIVE state
        // and scheduling the timer task
        for (final TimerImpl activeTimer : restorableTimers) {

            if (activeTimer.isAutoTimer()) {
                CalendarTimer calendarTimer = (CalendarTimer) activeTimer;
                boolean found = false;
                //so we know we have an auto timer. We need to try and match it up with the auto timers.
                ListIterator<AutoTimer> it = newAutoTimers.listIterator();
                while (it.hasNext()) {
                    AutoTimer timer = it.next();
                    if (doesTimeoutMethodMatch(calendarTimer.getTimeoutMethod(), timer.getMethod())) {

                        //the timers have the same method.
                        //now lets make sure the schedule is the same, info is the same,
                        // and the timer does not change the persistence
                        if (CalendarBasedTimeout.doesScheduleMatch(calendarTimer.getScheduleExpression(), timer.getScheduleExpression())
                                && Objects.equals(calendarTimer.info, timer.getTimerConfig().getInfo())
                                && timer.getTimerConfig().isPersistent()) {
                            it.remove();
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    activeTimer.setTimerState(TimerState.CANCELED, null);
                } else {
                    // ensure state switch to active if was TIMEOUT in the DB
                    // if the persistence is shared it must be ensured to not update
                    // timers of other nodes in the cluster
                    activeTimer.setTimerState(TimerState.ACTIVE, null);
                    calendarTimer.handleRestorationCalculation();
                }
                try {
                    this.persistTimer(activeTimer, false);
                } catch (Exception e) {
                    EJB3_TIMER_LOGGER.failedToPersistTimerOnStartup(activeTimer, e);
                }
                if (found) {
                    startTimer(activeTimer);
                    EJB3_TIMER_LOGGER.debugv("Started existing auto timer: {0}", activeTimer);
                }
            } else if (!TimerState.EXPIRED_CANCELED.contains(activeTimer.getState())) {
                startTimer(activeTimer);
            }
            EJB3_TIMER_LOGGER.debugv("Started timer: {0}", activeTimer);
        }

        for (AutoTimer timer : newAutoTimers) {
            this.loadAutoTimer(timer.getScheduleExpression(), timer.getTimerConfig(), timer.getMethod());
        }

    }

    /**
     * Registers a timer with a transaction (if any in progress) and then moves
     * the timer to an active state, so that it becomes eligible for timeouts
     */
    protected void startTimer(TimerImpl timer) {
        // if there's no transaction, then trigger a schedule immediately.
        // Else, the timer will be scheduled on tx synchronization callback
        if (!transactionActive()) {
            // set active if the timer is started if it was read
            // from persistence as current running to ensure correct schedule here
            timer.setTimerState(TimerState.ACTIVE, null);
            // create and schedule a timer task
            if (!this.registerTimerResource(timer)) {
                return;
            }
            timer.scheduleTimeout(true);
        } else {
            addWaitingOnTxCompletionTimer(timer);
            registerSynchronization(new TimerCreationTransactionSynchronization(timer));
        }
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
        // not a lifecycle callback
        return false;
    }

    /**
     * Creates and schedules a {@link TimerTask} for the next timeout of the passed <code>timer</code>
     */
    protected void scheduleTimeout(TimerImpl timer, boolean newTimer) {
        if (!newTimer && !scheduledTimerFutures.containsKey(timer.getId())) {
            //this timer has been cancelled by another thread. We just return
            return;
        }

        Date nextExpiration = timer.getNextExpiration();
        if (nextExpiration == null) {
            EJB3_TIMER_LOGGER.nextExpirationIsNull(timer);
            return;
        }
        // create the timer task
        final TimerTask timerTask = timer.getTimerTask();
        // find out how long is it away from now
        final long currentTime = System.currentTimeMillis();
        long delay = nextExpiration.getTime() - currentTime;
        long intervalDuration = timer.getInterval();
        final Task task = new Task(timerTask, ejbComponentInjectedValue.getValue().getControlPoint());
        if (intervalDuration > 0) {
            EJB3_TIMER_LOGGER.debugv("Scheduling timer {0} at fixed rate, starting at {1} milliseconds from now with repeated interval={2}",
                    timer, delay, intervalDuration);
            // if in past, then trigger immediately
            if (delay < 0) {
                delay = 0;
            }
        } else {
            EJB3_TIMER_LOGGER.debugv("Scheduling a single action timer {0} starting at {1} milliseconds from now", timer, delay);
            // if in past, then trigger immediately; if overdue by 5 minutes, set next expiration to current time
            if (delay < 0) {
                if (delay < -300000) {
                    timer.nextExpiration = new Date(currentTime);
                }
                delay = 0;
            }
        }
        final long delayFinal = delay;
        // maintain it in timerservice for future use (like cancellation)
        scheduledTimerFutures.compute(timer.getId(), (k, v) -> {
            if (timer.isCanceled()) {
                return null;
            } else {
                // schedule the task
                if (intervalDuration > 0) {
                    this.timerInjectedValue.getValue().scheduleAtFixedRate(task, delayFinal, intervalDuration);
                } else {
                    this.timerInjectedValue.getValue().schedule(task, delayFinal);
                }
                return task;
            }
        });
    }

    /**
     * Cancels any scheduled {@link java.util.concurrent.Future} corresponding to the passed <code>timer</code>
     *
     * @param timer the timer to cancel
     */
    protected void cancelTimeout(final TimerImpl timer) {
        scheduledTimerFutures.computeIfPresent(timer.getId(), (k, v) -> {
            v.cancel();
            return null;
        });
    }


    public boolean isScheduled(final String tid) {
        return this.scheduledTimerFutures.containsKey(tid);
    }

    /**
     * Returns an unmodifiable view of timers in the current transaction that are waiting for the transaction
     * to finish
     */
    private Map<String, TimerImpl> getWaitingOnTxCompletionTimers() {
        Map<String, TimerImpl> timers = null;
        if (getTransaction() != null) {
            timers = (Map<String, TimerImpl>) tsr.getResource(waitingOnTxCompletionKey);
        }
        return timers == null ? Collections.<String, TimerImpl>emptyMap() : timers;
    }

    private void addWaitingOnTxCompletionTimer(final TimerImpl timer) {
        Map<String, TimerImpl> timers = (Map<String, TimerImpl>) tsr.getResource(waitingOnTxCompletionKey);
        if (timers == null) {
            tsr.putResource(waitingOnTxCompletionKey, timers = new HashMap<String, TimerImpl>());
        }
        timers.put(timer.getId(), timer);
    }

    private boolean isSingletonBeanInvocation() {
        return ejbComponentInjectedValue.getValue() instanceof SingletonComponent;
    }

    private List<TimerImpl> getActivePersistentTimers() {
        // we need only those timers which correspond to the
        // timed object invoker to which this timer service belongs. So
        // first get hold of the timed object id
        final String timedObjectId = this.getInvoker().getTimedObjectId();
        // timer states which do *not* represent an active timer
        if (timerPersistence.getOptionalValue() == null) {
            //if the persistence setting is null then there are no persistent timers
            return Collections.emptyList();
        }
        final ContextTransactionManager transactionManager = ContextTransactionManager.getInstance();
        List<TimerImpl> persistedTimers;
        try {
            transactionManager.begin();
            persistedTimers = timerPersistence.getValue().loadActiveTimers(timedObjectId, this);
            transactionManager.commit();
        } catch (Exception e) {
            try {
                transactionManager.rollback();
            } catch (Exception ee) {
                // omit;
            }
            persistedTimers = Collections.emptyList();
            EJB3_TIMER_LOGGER.timerReinstatementFailed(timedObjectId, "unavailable", e);
        }
        final List<TimerImpl> activeTimers = new ArrayList<TimerImpl>();
        for (final TimerImpl persistedTimer : persistedTimers) {
            if (TimerState.EXPIRED_CANCELED.contains(persistedTimer.getState())) {
                continue;
            }
            // add it to the list of timers which will be restored
            activeTimers.add(persistedTimer);
        }

        return activeTimers;
    }

    private static boolean doesTimeoutMethodMatch(final Method timeoutMethod, final Method method2) {
        if (timeoutMethod.getName().equals(method2.getName())) {
            if (timeoutMethod.getParameterCount() == 0 && method2.getParameterCount() == 0) {
                return true;
            }
            if (timeoutMethod.getParameterCount() == 1 && method2.getParameterCount() == 1) {
                return timeoutMethod.getParameterTypes()[0] == method2.getParameterTypes()[0];
            }
        }
        return false;
    }

    /**
     * Marks the transaction for rollback
     * NOTE: This method will soon be removed, once this timer service
     * implementation becomes "managed"
     */
    private void setRollbackOnly() {
        try {
            Transaction tx = ContextTransactionManager.getInstance().getTransaction();
            if (tx != null) {
                tx.setRollbackOnly();
            }
        } catch (IllegalStateException ise) {
            EJB3_TIMER_LOGGER.ignoringException(ise);
        } catch (SystemException se) {
            EJB3_TIMER_LOGGER.ignoringException(se);
        }
    }

    private void assertTimerServiceState() {
        AllowedMethodsInformation.checkAllowed(MethodType.TIMER_SERVICE_METHOD);
        if (isLifecycleCallbackInvocation() && !this.isSingletonBeanInvocation()) {
            throw EJB3_TIMER_LOGGER.failToInvokeTimerServiceDoLifecycle();
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

    public TimerServiceResource getResource() {
        return resource;
    }

    private boolean registerTimerResource(final TimerImpl timer) {
        final TimerImpl previousValue = this.timers.putIfAbsent(timer.getId(), timer);
        if (previousValue != null) {
            return false;
        }
        this.resource.timerCreated(timer.getId());
        return true;
    }

    private void unregisterTimerResource(final String timerId) {
        this.timers.remove(timerId);
        this.resource.timerRemoved(timerId);
    }

    /**
     * Check if a persistent timer is already executed from a different instance
     * or should be executed.
     * For non-persistent timer it always return <code>true</code>.
     *
     * @param timer the timer which should be checked
     * @return <code>true</code> if the timer is not persistent or the persistent timer should start
     */
    public boolean shouldRun(TimerImpl timer) {
        // check peristent without further check to prevent from Exception (WFLY-6152)
        return !timer.persistent || timerPersistence.getValue().shouldRun(timer);
    }

    /**
     * Safely closes some resource without throwing an exception.
     * Any exception will be logged at TRACE level.
     *
     * @param resource the resource to close
     */
    public static void safeClose(final AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Throwable t) {
                EjbLogger.EJB3_TIMER_LOGGER.tracef(t, "Closing resource failed");
            }
        }
    }

    private class TimerCreationTransactionSynchronization implements Synchronization {
        /**
         * The timer being managed in the transaction
         */
        private final TimerImpl timer;

        public TimerCreationTransactionSynchronization(TimerImpl timer) {
            if (timer == null) {
                throw EJB3_TIMER_LOGGER.timerIsNull();
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
            if (status == Status.STATUS_COMMITTED) {
                EJB3_TIMER_LOGGER.debugv("commit timer creation: {0}", this.timer);

                if (!registerTimerResource(timer)) {
                    return;
                }
                TimerState timerState = this.timer.getState();
                switch (timerState) {
                    case CREATED:
                        this.timer.setTimerState(TimerState.ACTIVE, null);
                        this.timer.scheduleTimeout(true);
                        break;
                    case ACTIVE:
                        this.timer.scheduleTimeout(true);
                        break;
                }
            } else if (status == Status.STATUS_ROLLEDBACK) {
                EJB3_TIMER_LOGGER.debugv("Rolling back timer creation: {0}", this.timer);
                this.timer.setTimerState(TimerState.CANCELED, null);
            }
        }
    }


    private class TimerRemoveSynchronization implements Synchronization {

        private final TimerImpl timer;

        private TimerRemoveSynchronization(final TimerImpl timer) {
            this.timer = timer;
        }

        @Override
        public void beforeCompletion() {

        }

        @Override
        public void afterCompletion(final int status) {
            try {
                if (status == Status.STATUS_COMMITTED) {
                    cancelTimeout(timer);
                    unregisterTimerResource(timer.getId());
                } else {
                    timer.setTimerState(TimerState.ACTIVE, null);
                }
            } finally {
                timer.unlock();
            }
        }
    }

    private class TaskPostPersist extends java.util.TimerTask {
        private final TimerImpl timer;
        private long delta = 0;
        private long nextExpirationPristine = 0;

        TaskPostPersist(TimerImpl timer) {
            this.timer = timer;
            if (timer.nextExpiration != null) {
                this.nextExpirationPristine = timer.nextExpiration.getTime();
            }
        }

        TaskPostPersist(TimerImpl timer, long delta, long nextExpirationPristine) {
            this.timer = timer;
            this.delta = delta;
            this.nextExpirationPristine = nextExpirationPristine;
        }

        @Override
        public void run() {
            executorServiceInjectedValue.getValue().submit(this::persistTimer);
        }

        void persistTimer() {
            final ContextTransactionManager transactionManager = ContextTransactionManager.getInstance();
            try {
                transactionManager.begin();
                timerPersistence.getValue().persistTimer(timer);
                transactionManager.commit();
            } catch (Exception e) {
                try {
                    transactionManager.rollback();
                } catch (Exception ee) {
                    // omit;
                }
                EJB3_TIMER_LOGGER.exceptionPersistTimerState(timer, e);
                long nextExpirationDelay;
                if (nextExpirationPristine > 0 && timer.timerState != TimerState.RETRY_TIMEOUT &&
                        (nextExpirationDelay = nextExpirationPristine - System.currentTimeMillis()) > delta) {
                    if (delta == 0L) {
                        delta = nextExpirationDelay / (1L + MAX_RETRY.longValue());
                    }
                    timerInjectedValue
                            .getValue()
                            .schedule(new TaskPostPersist(timer, delta, nextExpirationPristine), delta);
                } else {
                    EJB3_TIMER_LOGGER.exceptionPersistPostTimerState(timer, e);
                }
            }
        }
    }

    private class Task extends java.util.TimerTask {

        private final TimerTask delegate;
        private final ControlPoint controlPoint;
        /**
         * This is true if a task is queued up to be run by the request controller,
         * used to stop timer tasks banking up when the container is suspended.
         */
        private volatile boolean queued = false;

        public Task(final TimerTask delegate, ControlPoint controlPoint) {
            this.delegate = delegate;
            this.controlPoint = controlPoint;
        }

        @Override
        public void run() {
            final ExecutorService executor = executorServiceInjectedValue.getOptionalValue();
            if (executor != null) {
                if (controlPoint == null) {
                    executor.submit(delegate);
                } else if (!queued) {
                    queued = true;
                    controlPoint.queueTask(new Runnable() {
                        @Override
                        public void run() {
                            queued = false;
                            delegate.run();
                        }
                    }, executor, -1, null, false);
                } else {
                    EjbLogger.EJB3_TIMER_LOGGER.debug("Skipping timer invocation as existing request is already queued.");
                }
            }
        }

        @Override
        public boolean cancel() {
            delegate.cancel();
            return super.cancel();
        }
    }

    private final class TimerRefreshListener implements TimerPersistence.TimerChangeListener {

        @Override
        public void timerAdded(TimerImpl timer) {
            TimerServiceImpl.this.startTimer(timer);
        }

        @Override
        public void timerRemoved(String timerId) {
            TimerImpl timer = TimerServiceImpl.this.getTimer(timerId);
            if (timer != null) {
                TimerServiceImpl.this.cancelTimeout(timer);
                TimerServiceImpl.this.unregisterTimerResource(timer.getId());
            }
        }

        @Override
        public TimerServiceImpl getTimerService() {
            return TimerServiceImpl.this;
        }

        @Override
        public void timerSync(TimerImpl oldTimer, TimerImpl newTimer) {
            TimerServiceImpl.this.removeTimer(oldTimer);
            TimerServiceImpl.this.startTimer(newTimer);
        }
    }


}
