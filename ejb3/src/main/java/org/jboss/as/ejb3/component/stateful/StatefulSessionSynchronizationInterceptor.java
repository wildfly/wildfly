/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateful;

import static org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance.SYNC_STATE_AFTER_COMPLETE_DELAYED_COMMITTED;
import static org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance.SYNC_STATE_AFTER_COMPLETE_DELAYED_NO_COMMIT;
import static org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance.SYNC_STATE_INVOCATION_IN_PROGRESS;
import static org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance.SYNC_STATE_NO_INVOCATION;
import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ejb.EJBException;
import jakarta.ejb.TransactionManagementType;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstanceInterceptorFactory;
import org.jboss.as.ejb3.component.interceptors.AbstractEJBInterceptor;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBean;
import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.tx.OwnableReentrantLock;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * {@link org.jboss.invocation.Interceptor} which manages {@link Synchronization} semantics on a stateful session bean.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Jaikiran Pai
 */
public class StatefulSessionSynchronizationInterceptor extends AbstractEJBInterceptor {

    private final boolean containerManagedTransactions;

    private static final Factory CONTAINER_MANAGED = new Factory(TransactionManagementType.CONTAINER);
    private static final Factory BEAN_MANAGED = new Factory(TransactionManagementType.BEAN);

    public static InterceptorFactory factory(final TransactionManagementType type) {
        //we need to always return the same factory instance
        //otherwise multiple synchronization interceptors may be created
        return type == TransactionManagementType.CONTAINER ? CONTAINER_MANAGED : BEAN_MANAGED;
    }


    public StatefulSessionSynchronizationInterceptor(final boolean containerManagedTransactions) {
        this.containerManagedTransactions = containerManagedTransactions;
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final StatefulSessionComponent component = getComponent(context, StatefulSessionComponent.class);
        final StatefulSessionBean<SessionID, StatefulSessionComponentInstance> bean = StatefulComponentInstanceInterceptor.getBean(context);
        final StatefulSessionComponentInstance instance = bean.getInstance();

        final OwnableReentrantLock lock = instance.getLock();
        final Object threadLock = instance.getThreadLock();
        final AtomicInteger invocationSyncState = instance.getInvocationSyncState();

        final TransactionSynchronizationRegistry tsr = component.getTransactionSynchronizationRegistry();
        final Object lockOwner = getLockOwner(tsr);
        final AccessTimeoutDetails timeout = component.getAccessTimeout(context.getMethod());
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.trace("Trying to acquire lock: " + lock + " for stateful component instance: " + instance + " during invocation: " + context);
        }
        // we obtain a lock in this synchronization interceptor because the lock needs to be tied to the synchronization
        // so that it can released on the tx synchronization callbacks
        boolean acquired = lock.tryLock(timeout.getValue(), timeout.getTimeUnit(), lockOwner);
        if (!acquired) {
            throw EjbLogger.ROOT_LOGGER.failToObtainLock(component.getComponentName(), timeout.getValue(), timeout.getTimeUnit());
        }
        synchronized (threadLock) {
            invocationSyncState.set(SYNC_STATE_INVOCATION_IN_PROGRESS); //invocation in progress
            if (ROOT_LOGGER.isTraceEnabled()) {
                ROOT_LOGGER.trace("Acquired lock: " + lock + " for stateful component instance: " + instance + " during invocation: " + context);
            }

            // If using CMT, get the key to current transaction associated with this thread
            // we never register a sync for bean managed transactions
            // the inner BMT interceptor is going to setup the correct transaction anyway
            // so enrolling in an existing transaction is not correct
            Object currentTransactionKey = this.containerManagedTransactions ? tsr.getTransactionKey() : null;
            boolean wasTxSyncRegistered = false;
            try {
                if ((currentTransactionKey != null) && tsr.getResource(bean.getId()) == null) {
                    final int status = tsr.getTransactionStatus();
                    // if this SFSB instance is already associated with a different transaction, then it's an error
                    // if the thread is currently associated with a tx, then register a tx synchronization
                    if (status != Status.STATUS_COMMITTED && status != Status.STATUS_ROLLEDBACK) {
                        // register a tx synchronization for this SFSB instance
                        final Synchronization statefulSessionSync = new StatefulSessionSynchronization(bean);
                        tsr.registerInterposedSynchronization(statefulSessionSync);
                        wasTxSyncRegistered = true;
                        if (ROOT_LOGGER.isTraceEnabled()) {
                            ROOT_LOGGER.trace("Registered tx synchronization: " + statefulSessionSync + " for tx: " + currentTransactionKey +
                                    " associated with stateful component instance: " + instance);
                        }
                        // invoke the afterBegin callback on the SFSB
                        instance.afterBegin();
                        // Retain reference to bean for all invocations within tx
                        tsr.putResource(bean.getId(), bean);
                        context.putPrivateData(StatefulTransactionMarker.class, StatefulTransactionMarker.of(true));
                    }
                } else {
                    context.putPrivateData(StatefulTransactionMarker.class, StatefulTransactionMarker.of(false));
                }
                // proceed with the invocation
                // handle exceptions to coincide with exception handling in StatefulComponentInstanceInterceptor
                try {
                    return context.proceed();
                } catch (Exception ex) {
                    if (component.shouldDiscard(ex, context.getMethod())) {
                        bean.discard();
                    }
                    throw ex;
                } catch (Throwable t) {
                    // discard bean cache state on error
                    bean.discard();
                    throw t;
                }
            } finally {
                // if the current call did *not* register a tx SessionSynchronization, then we have to explicitly mark the
                // SFSB instance as "no longer in use". If it registered a tx SessionSynchronization, then releasing the lock is
                // taken care off by a tx synchronization callbacks.
                // case: sync was not registered in this invocation nor in a previous one
                if (!wasTxSyncRegistered) {
                    if ((currentTransactionKey == null) || (tsr.getResource(bean.getId()) == null)) {
                        ROOT_LOGGER.tracef("Calling release from synchronization interceptor (#1), instance id K = %s", instance.getId());
                        close(bean);
                    } else {
                        // case: sync was not registered in this invocation but in a previous one
                        // if we don't release the lock here then it will be acquired multiple times and only released once
                        // The StatefulSessionBean  will be closed by its synchronization
                        releaseLock(instance);
                    }
                }
                for(;;) {
                    int state = invocationSyncState.get();
                    if(state == SYNC_STATE_INVOCATION_IN_PROGRESS && invocationSyncState.compareAndSet(SYNC_STATE_INVOCATION_IN_PROGRESS, SYNC_STATE_NO_INVOCATION)) {
                        break;
                    } else if (state == SYNC_STATE_AFTER_COMPLETE_DELAYED_COMMITTED || state == SYNC_STATE_AFTER_COMPLETE_DELAYED_NO_COMMIT) {
                        try {
                            //invoke the after completion method, other after completion syncs may have already run
                            handleAfterCompletion(state == SYNC_STATE_AFTER_COMPLETE_DELAYED_COMMITTED, bean);
                        } finally {
                            invocationSyncState.set(SYNC_STATE_NO_INVOCATION);
                        }
                    } else {
                        EjbLogger.ROOT_LOGGER.unexpectedInvocationState(state);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Use either the active transaction or the current thread as the lock owner
     *
     * @return The lock owner
     */
    private static Object getLockOwner(final TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
        Object owner = transactionSynchronizationRegistry.getTransactionKey();
        return owner != null ? owner : Thread.currentThread();
    }

    /**
     * Closes the specified {@link StatefulSessionBean} and releases the lock, held by this thread, on the stateful component instance.
     *
     * @param instance The stateful component instance
     */
    static void close(StatefulSessionBean<SessionID, StatefulSessionComponentInstance> bean) {
        StatefulSessionComponentInstance instance = bean.getInstance();
        try {
            bean.close();
        } finally {
            // release the lock on the SFSB instance
            releaseLock(instance);
        }
    }

    /**
     * Releases the lock, held by this thread, on the stateful component instance.
     */
    static void releaseLock(final StatefulSessionComponentInstance instance) {
        instance.getLock().unlock(getLockOwner(instance.getComponent().getTransactionSynchronizationRegistry()));
        ROOT_LOGGER.tracef("Released lock: %s", instance.getLock());
    }

    private static class Factory extends ComponentInstanceInterceptorFactory {

        private final TransactionManagementType type;

        public Factory(final TransactionManagementType type) {
            this.type = type;
        }

        @Override
        protected Interceptor create(final Component component, final InterceptorFactoryContext context) {
            return new StatefulSessionSynchronizationInterceptor(type == TransactionManagementType.CONTAINER );
        }
    }


    private static final class StatefulSessionSynchronization implements Synchronization {

        private final StatefulSessionBean<SessionID, StatefulSessionComponentInstance> bean;

        StatefulSessionSynchronization(StatefulSessionBean<SessionID, StatefulSessionComponentInstance> bean) {
            this.bean = bean;
        }

        @Override
        public void beforeCompletion() {
            StatefulSessionComponentInstance instance = this.bean.getInstance();
            try {
                ROOT_LOGGER.tracef("Before completion callback invoked on Transaction synchronization: %s of stateful component instance: %s" , this, instance);

                if (!this.bean.isDiscarded()) {
                    instance.beforeCompletion();
                }
            } catch (Throwable t) {
                handleThrowable(t, this.bean);
            }
        }

        @Override
        public void afterCompletion(int status) {
            StatefulSessionComponentInstance instance = this.bean.getInstance();
            AtomicInteger state = instance.getInvocationSyncState();
            final boolean committed = status == Status.STATUS_COMMITTED;
            for(;;) {
                int s = state.get();
                if (s == SYNC_STATE_NO_INVOCATION) {
                    handleAfterCompletion(committed, this.bean);
                    break;
                } else if (s == SYNC_STATE_INVOCATION_IN_PROGRESS  && state.compareAndSet(SYNC_STATE_INVOCATION_IN_PROGRESS, committed ? SYNC_STATE_AFTER_COMPLETE_DELAYED_COMMITTED : SYNC_STATE_AFTER_COMPLETE_DELAYED_NO_COMMIT)) {
                    break;
                }
            }
        }
    }

    static void handleAfterCompletion(boolean committed, StatefulSessionBean<SessionID, StatefulSessionComponentInstance> bean) {
        StatefulSessionComponentInstance instance = bean.getInstance();
        try {
            ROOT_LOGGER.tracef("After completion callback invoked on Transaction synchronization: %s", instance);

            if (!bean.isDiscarded()) {
                instance.afterCompletion(committed);
            }
        } catch (Throwable t) {
            handleThrowable(t, bean);
        }
        // Bean removal within a tx deferred destroy to its Synchronization
        if (bean.isRemoved()) {
            try {
                instance.destroy();
            } catch (Throwable t) {
                handleThrowable(t, bean);
            }
        }

        // tx has completed, so close the StatefulSessionBean and unlock the instance lock
        instance.getComponent().getTransactionSynchronizationRegistry().putResource(bean.getId(), null);
        close(bean);
    }


    private static void handleThrowable(Throwable t, StatefulSessionBean<SessionID, StatefulSessionComponentInstance> bean) {
        StatefulSessionComponentInstance instance = bean.getInstance();
        ROOT_LOGGER.discardingStatefulComponent(instance, t);
        try {
            // discard the SFSB instance
            bean.discard();
        } finally {
            // release the lock associated with the SFSB instance
            releaseLock(instance);
        }
        // throw back an appropriate exception
        if (t instanceof RuntimeException)
            throw (RuntimeException) t;
        if (t instanceof Error)
            throw (Error) t;
        throw (EJBException) new EJBException().initCause(t);
    }
}
