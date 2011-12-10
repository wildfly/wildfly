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
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.Date;

import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.as.ejb3.timerservice.persistence.TimerEntity;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.as.ejb3.timerservice.task.TimerTask;
import org.jboss.invocation.InterceptorContext;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * Implementation of EJB3.1 {@link Timer}
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class TimerImpl implements Timer {

    /**
     * Unique id for this timer instance
     */
    protected final String id;

    /**
     * The timer state
     */
    protected volatile TimerState timerState;

    /**
     * The {@link javax.ejb.TimerService} through which this timer was created
     */
    protected final TimerServiceImpl timerService;

    /**
     * The {@link TimedObjectInvoker} to which this timer corresponds
     */
    protected final TimedObjectInvoker timedObjectInvoker;

    /**
     * The info which was passed while creating the timer.
     */
    protected final Serializable info;

    /**
     * Indicates whether the timer is persistent
     */
    protected final boolean persistent;

    /**
     * A {@link javax.ejb.TimerHandle} for this timer
     */
    protected final TimerHandleImpl handle;

    /**
     * The initial (first) expiry date of this timer
     */
    protected final Date initialExpiration;

    /**
     * The duration in milli sec. between timeouts
     */
    protected final long intervalDuration;

    /**
     * If this is an entity bean then this is the primary key
     */
    protected final Object primaryKey;

    /**
     * Next expiry date of this timer
     */
    protected volatile Date nextExpiration;

    /**
     * The date of the previous run of this timer
     */
    protected volatile Date previousRun;

    /**
     * If the timer is persistent, then this represents its persistent state.
     */
    protected volatile TimerEntity persistentState;

    /**
     * Creates a {@link TimerImpl}
     *
     * @param id               The id of this timer
     * @param service          The timer service through which this timer was created
     * @param initialExpiry    The first expiry of this timer
     * @param intervalDuration The duration (in milli sec) between timeouts
     * @param info             The info that will be passed on through the {@link Timer} and will be available through the {@link Timer#getInfo()} method
     * @param persistent       True if this timer is persistent. False otherwise
     */
    public TimerImpl(String id, TimerServiceImpl service, Date initialExpiry, long intervalDuration, Serializable info,
                     boolean persistent, Object primaryKey) {
        this(id, service, initialExpiry, intervalDuration, initialExpiry, info, persistent, primaryKey);
    }

    /**
     * Creates a {@link TimerImpl}
     *
     * @param id               The id of this timer
     * @param service          The timer service through which this timer was created
     * @param initialExpiry    The first expiry of this timer. Can be null
     * @param intervalDuration The duration (in milli sec) between timeouts
     * @param nextEpiry        The next expiry of this timer
     * @param info             The info that will be passed on through the {@link Timer} and will be available through the {@link Timer#getInfo()} method
     * @param persistent       True if this timer is persistent. False otherwise
     */
    public TimerImpl(String id, TimerServiceImpl service, Date initialExpiry, long intervalDuration, Date nextEpiry,
                     Serializable info, boolean persistent, Object primaryKey) {
        assert service != null : "service is null";
        assert id != null : "id is null";

        this.id = id;
        this.timerService = service;

        this.timedObjectInvoker = service.getInvoker();
        this.info = info;
        this.persistent = persistent;
        this.initialExpiration = initialExpiry;
        this.intervalDuration = intervalDuration;
        this.nextExpiration = nextEpiry;
        this.previousRun = null;
        this.primaryKey = primaryKey;

        // create a timer handle for this timer
        this.handle = new TimerHandleImpl(this.id, this.timedObjectInvoker.getTimedObjectId(), service);

        setTimerState(TimerState.CREATED);

    }

    /**
     * Creates a {@link TimerImpl} out of a persisted timer
     *
     * @param persistedTimer The persisted state of the timer
     * @param service        The timer service to which this timer belongs
     */
    public TimerImpl(TimerEntity persistedTimer, TimerServiceImpl service) {
        this(persistedTimer.getId(), service, persistedTimer.getInitialDate(), persistedTimer.getInterval(),
                persistedTimer.getNextDate(), persistedTimer.getInfo(), true, persistedTimer.getPrimaryKey());
        this.previousRun = persistedTimer.getPreviousRun();
        this.timerState = persistedTimer.getTimerState();
    }

    /**
     * Returns the id of this timer
     *
     * @return
     */
    public String getId() {
        return this.id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCalendarTimer() throws IllegalStateException, EJBException {
        // first check whether this timer has expired or cancelled
        this.assertTimerState();

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() throws IllegalStateException, EJBException {
        // first check whether the timer has expired or has been cancelled
        this.assertTimerState();
        if (timerState != TimerState.EXPIRED) {
            setTimerState(TimerState.CANCELED);
        }
        if (timerService.transactionActive()) {
            final Transaction currentTx = this.timerService.getTransaction();
            this.registerTimerCancellationWithTx(currentTx);
        } else {
            // cancel any scheduled Future for this timer
            this.cancelTimeout();
            // persist changes
            timerService.persistTimer(this);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see #getTimerHandle()
     */
    @Override
    public TimerHandle getHandle() throws IllegalStateException, EJBException {
        // make sure it's in correct state
        this.assertTimerState();

        // for non-persistent timers throws an exception (mandated by EJB3 spec)
        if (this.persistent == false) {
            throw MESSAGES.invalidTimerHandlersForPersistentTimers("EJB3.1 Spec 18.2.6");
        }
        return this.handle;
    }

    /**
     * This method returns the {@link javax.ejb.TimerHandle} corresponding to this {@link TimerImpl}.
     * Unlike the {@link #getHandle()} method, this method does <i>not</i> throw an {@link IllegalStateException}
     * or {@link javax.ejb.NoSuchObjectLocalException} or {@link javax.ejb.EJBException}, for non-persistent timers.
     * Instead this method returns the {@link javax.ejb.TimerHandle} corresponding to that non-persistent
     * timer (remember that {@link TimerImpl} creates {@link javax.ejb.TimerHandle} for both persistent and non-persistent timers)
     *
     * @return
     */
    public TimerHandle getTimerHandle() {
        return this.handle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPersistent() throws IllegalStateException, EJBException {
        // make sure the call is allowed in the current timer state
        this.assertTimerState();

        return this.persistent;
    }

    /**
     * {@inheritDoc}
     *
     * @see #getTimerInfo()
     */
    @Override
    public Serializable getInfo() throws IllegalStateException, EJBException {
        // make sure this call is allowed
        this.assertTimerState();

        return this.info;
    }

    /**
     * This method is similar to {@link #getInfo()}, except that this method does <i>not</i> check the timer state
     * and hence does <i>not</i> throw either {@link IllegalStateException} or {@link javax.ejb.NoSuchObjectLocalException}
     * or {@link javax.ejb.EJBException}.
     *
     * @return
     */
    public Serializable getTimerInfo() {
        return this.info;
    }

    /**
     * {@inheritDoc}
     *
     * @see #getNextExpiration()
     */
    @Override
    public Date getNextTimeout() throws IllegalStateException, EJBException {
        // first check the validity of the timer state
        this.assertTimerState();
        if (this.nextExpiration == null) {
            throw MESSAGES.noMoreTimeoutForTimer(this);
        }
        return this.nextExpiration;
    }

    /**
     * This method is similar to {@link #getNextTimeout()}, except that this method does <i>not</i> check the timer state
     * and hence does <i>not</i> throw either {@link IllegalStateException} or {@link javax.ejb.NoSuchObjectLocalException}
     * or {@link javax.ejb.EJBException}.
     *
     * @return
     */
    public Date getNextExpiration() {
        return this.nextExpiration;
    }

    /**
     * Sets the next timeout of this timer
     *
     * @param next The next scheduled timeout of this timer
     */
    public void setNextTimeout(Date next) {
        this.nextExpiration = next;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduleExpression getSchedule() throws IllegalStateException, EJBException {
        this.assertTimerState();
        throw MESSAGES.invalidTimerNotCalendarBaseTimer(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeRemaining() throws IllegalStateException, EJBException {
        // TODO: Rethink this implementation

        // first check the validity of the timer state
        this.assertTimerState();
        if (this.nextExpiration == null) {
            throw MESSAGES.noMoreTimeoutForTimer(this);
        }
        long currentTimeInMillis = System.currentTimeMillis();
        long nextTimeoutInMillis = this.nextExpiration.getTime();

        // if the next expiration is *not* in future and the repeat interval isn't
        // a positive number (i.e. no repeats) then there won't be any more timeouts.
        // So throw a NoMoreTimeoutsException.
        // NOTE: We check for intervalDuration and not just nextExpiration because,
        // it's a valid case where the nextExpiration is in past (maybe the server was
        // down when the timeout was expected)
        //      if (nextTimeoutInMillis < currentTimeInMillis && this.intervalDuration <= 0)
        //      {
        //         throw new NoMoreTimeoutsException("No more timeouts for timer " + this);
        //      }
        return nextTimeoutInMillis - currentTimeInMillis;
    }

    public boolean isAutoTimer() {
        return false;
    }

    /**
     * Cancels any scheduled timer task for this timer
     */
    protected void cancelTimeout() {
        // delegate to the timerservice, so that it can cancel any scheduled Future
        // for this timer
        this.timerService.cancelTimeout(this);
    }

    /**
     * Returns the initial (first) timeout date of this timer
     *
     * @return
     */
    public Date getInitialExpiration() {
        return this.initialExpiration;
    }

    /**
     * Returns the interval (in milli seconds), between timeouts, of this timer.
     *
     * @return
     */
    public long getInterval() {
        return this.intervalDuration;
    }

    /**
     * Returns the timed object id to which this timer belongs
     *
     * @return
     */
    public String getTimedObjectId() {
        return this.timerService.getInvoker().getTimedObjectId();
    }

    /**
     * Returns the timer service through which this timer was created
     *
     * @return
     */
    public TimerServiceImpl getTimerService() {
        return this.timerService;
    }

    /**
     * Returns true if this timer is active. Else returns false.
     * <p>
     * A timer is considered to be "active", if its {@link TimerState}
     * is neither of the following:
     * <ul>
     * <li>{@link TimerState#CANCELED}</li>
     * <li>{@link TimerState#EXPIRED}</li>
     * </ul>
     * <p/>
     * And if the corresponding timer service is still up
     * <p/>
     * </p>
     *
     * @return
     */
    public boolean isActive() {
        return timerService.isStarted() && !isCanceled() && !isExpired();
    }

    /**
     * Returns true if this timer is in {@link TimerState#CANCELED} state. Else returns false.
     *
     * @return
     */
    public boolean isCanceled() {
        return timerState == TimerState.CANCELED;
    }

    /**
     * Returns true if this timer is in {@link TimerState#EXPIRED} state. Else returns false
     *
     * @return
     */
    public boolean isExpired() {
        return timerState == TimerState.EXPIRED;
    }

    /**
     * Returns true if this timer is in {@link TimerState#RETRY_TIMEOUT}. Else returns false.
     *
     * @return
     */
    public boolean isInRetry() {
        return timerState == TimerState.RETRY_TIMEOUT;
    }

    /**
     * Returns the {@link java.util.Date} of the previous timeout of this timer
     *
     * @return
     */
    public Date getPreviousRun() {
        return this.previousRun;
    }

    /**
     * Sets the {@link java.util.Date} of the previous timeout of this timer
     *
     * @param previousRun
     */
    public void setPreviousRun(Date previousRun) {
        this.previousRun = previousRun;
    }

    /**
     * Returns the current state of this timer
     *
     * @return
     */
    public TimerState getState() {
        return this.timerState;
    }

    /**
     * Asserts that the timer is <i>not</i> in any of the following states:
     * <ul>
     * <li>{@link TimerState#CANCELED}</li>
     * <li>{@link TimerState#EXPIRED}</li>
     * </ul>
     *
     * @throws javax.ejb.NoSuchObjectLocalException
     *          if the txtimer was canceled or has expired
     */
    protected void assertTimerState() {
        if (timerState == TimerState.EXPIRED)
            throw MESSAGES.timerHasExpired();
        if (timerState == TimerState.CANCELED)
            throw MESSAGES.timerWasCanceled();
        final InterceptorContext ctx = CurrentInvocationContext.get();
        if (ctx != null) {
            if (ctx.getPrivateData(Component.class) instanceof StatefulSessionComponent) {
                if (ctx.getMethod() == null) {
                    throw new IllegalStateException("Timer methods may not be invoked from lifecycle methods of a stateful session bean");
                }
            }
        }
        TimerServiceDisabledTacker.assertEnabled();
    }

    /**
     * Expire, and remove it from the timer service.
     */
    public void expireTimer() {
        ROOT_LOGGER.debug("expireTimer: " + this);
        setTimerState(TimerState.EXPIRED);
        // remove from timerservice
        timerService.removeTimer(this);
        // Cancel any scheduled timer task for this timer
        this.cancelTimeout();
    }

    /**
     * Sets the state of this timer
     *
     * @param state The state of this timer
     */
    public void setTimerState(TimerState state) {
        this.timerState = state;
    }

    /**
     * Returns the current persistent state of this timer
     */
    public TimerEntity getPersistentState() {
        if (this.persistent == false) {
            throw MESSAGES.failToPersistTimer(this);
        }
        if (this.persistentState == null) {
            // create a new new persistent state
            this.persistentState = this.createPersistentState();
        } else {
            // just refresh the fields which change in the persistent timer
            this.persistentState.setNextDate(this.nextExpiration);
            this.persistentState.setPreviousRun(this.previousRun);
            this.persistentState.setTimerState(this.timerState);
        }
        return this.persistentState;
    }

    /**
     * Suspends any currently scheduled task for this timer
     * <p>
     * Note that, suspend does <b>not</b> cancel the {@link Timer}. Instead,
     * it just cancels the <b>next scheduled timeout</b>. So once the {@link Timer}
     * is restored (whenever that happens), the {@link Timer} will continue to
     * timeout at appropriate times.
     * </p>
     */
    // TODO: Revisit this method, we probably don't need this any more.
    // In terms of implementation, this is just equivalent to cancelTimeout() method
    public void suspend() {
        // cancel any scheduled timer task (Future) for this timer
        this.cancelTimeout();
    }

    /**
     * Creates and schedules a {@link TimerTask} for the next timeout of this timer
     */
    public void scheduleTimeout() {
        // just delegate to timerservice, for it to do the actual scheduling
        this.timerService.scheduleTimeout(this);
    }

    /**
     * Creates and returns a new persistent state of this timer
     *
     * @return
     */
    protected TimerEntity createPersistentState() {
        return new TimerEntity(this);
    }

    /**
     * Returns the task which handles the timeouts of this {@link TimerImpl}
     *
     * @return
     * @see TimerTask
     */
    protected TimerTask<?> getTimerTask() {
        return new TimerTask<TimerImpl>(this);
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
        if (obj instanceof TimerImpl == false) {
            return false;
        }
        TimerImpl otherTimer = (TimerImpl) obj;
        return this.handle.equals(otherTimer.getTimerHandle());
    }

    @Override
    public int hashCode() {
        return this.handle.hashCode();
    }


    /**
     * A nice formatted string output for this timer
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        //TODO: Cache this
        StringBuilder sb = new StringBuilder();
        sb.append("[id=");
        sb.append(this.id);
        sb.append(" ");
        sb.append("timedObjectId=");
        if (this.timedObjectInvoker == null) {
            sb.append("null");
        } else {
            sb.append(this.timedObjectInvoker.getTimedObjectId());
        }
        sb.append(" ");
        sb.append("auto-timer?:");
        sb.append(this.isAutoTimer());
        sb.append(" ");
        sb.append("persistent?:");
        sb.append(this.persistent);
        sb.append(" ");
        sb.append("timerService=");
        sb.append(this.timerService);
        sb.append(" ");
        sb.append("initialExpiration=");
        sb.append(this.initialExpiration);
        sb.append(" ");
        sb.append("intervalDuration(in milli sec)=");
        sb.append(this.intervalDuration);
        sb.append(" ");
        sb.append("nextExpiration=");
        sb.append(this.nextExpiration);
        sb.append(" ");
        sb.append("timerState=");
        sb.append(this.timerState);

        return sb.toString();
    }

    private void registerTimerCancellationWithTx(Transaction tx) {
        try {
            tx.registerSynchronization(new TimerCancellationTransactionSynchronization(this));
        } catch (Exception e) {
            throw MESSAGES.failToRegisterWithTxTimerCancellation(e);
        }
    }

    private Serializable deserialize(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStreamWithTCCL(bais);
            return (Serializable) ois.readObject();
        } catch (IOException ioe) {
            throw MESSAGES.failToDeserializeInfoInTimer(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw MESSAGES.failToDeserializeInfoInTimer(cnfe);
        }
    }

    /**
     * {@link ObjectInputStreamWithTCCL} during {@link #resolveClass(java.io.ObjectStreamClass)}
     * first tries to resolve the class in the thread context classloader
     * {@link Thread#getContextClassLoader()}. If it cannot resolve in the current context
     * loader, it passes on the control to {@link java.io.ObjectInputStream} to resolve the class
     */
    private final class ObjectInputStreamWithTCCL extends ObjectInputStream {

        public ObjectInputStreamWithTCCL(InputStream in) throws IOException {
            super(in);
        }

        protected Class<?> resolveClass(ObjectStreamClass v) throws IOException, ClassNotFoundException {
            String className = v.getName();
            Class<?> resolvedClass = null;

            ROOT_LOGGER.trace("Attempting to locate class [" + className + "]");

            try {
                resolvedClass = timedObjectInvoker.getClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                resolvedClass = getClass().getClassLoader().loadClass(className);
            }
            return resolvedClass;
        }
    }

    private class TimerCancellationTransactionSynchronization implements Synchronization {

        /**
         * The timer being managed in the transaction
         */
        private TimerImpl timer;

        public TimerCancellationTransactionSynchronization(TimerImpl timer) {
            if (timer == null) {
                throw MESSAGES.timerIsNull();
            }
            this.timer = timer;
        }

        @Override
        public void afterCompletion(int status) {
            if (status == Status.STATUS_COMMITTED) {
                ROOT_LOGGER.debug("commit timer cancellation: " + this.timer);

                final TimerState timerState = this.timer.getState();
                switch (timerState) {
                    case CANCELED:
                    case IN_TIMEOUT:
                    case RETRY_TIMEOUT:
                        this.timer.cancelTimeout();
                        timerService.persistTimer(timer);
                        break;

                }
            } else if (status == Status.STATUS_ROLLEDBACK) {
                ROOT_LOGGER.debug("rollback timer cancellation: " + this.timer);

                TimerState timerState = this.timer.getState();
                switch (timerState) {
                    case CANCELED:
                        this.timer.setTimerState(TimerState.ACTIVE);
                        break;

                }

            }
        }

        @Override
        public void beforeCompletion() {
            // TODO Auto-generated method stub

        }

    }

    public Object getPrimaryKey() {
        return primaryKey;
    }
}
