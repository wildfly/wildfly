/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component.stateful;

import javax.ejb.EJBException;
import javax.ejb.TransactionManagementType;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstanceInterceptorFactory;
import org.jboss.as.ejb3.component.interceptors.AbstractEJBInterceptor;
import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.as.ejb3.tx.OwnableReentrantLock;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;
import static org.jboss.as.ejb3.component.stateful.StatefulComponentInstanceInterceptor.getComponentInstance;

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
        final StatefulSessionComponentInstance instance = getComponentInstance(context);

        final OwnableReentrantLock lock = instance.getLock();
        final Object threadLock = instance.getThreadLock();

        final TransactionSynchronizationRegistry transactionSynchronizationRegistry = component.getTransactionSynchronizationRegistry();
        final Object lockOwner = getLockOwner(transactionSynchronizationRegistry);
        lock.pushOwner(lockOwner);
        try {
            final AccessTimeoutDetails timeout = component.getAccessTimeout(context.getMethod());
            if (ROOT_LOGGER.isTraceEnabled()) {
                ROOT_LOGGER.trace("Trying to acquire lock: " + lock + " for stateful component instance: " + instance + " during invocation: " + context);
            }
            // we obtain a lock in this synchronization interceptor because the lock needs to be tied to the synchronization
            // so that it can released on the tx synchronization callbacks
            boolean acquired = lock.tryLock(timeout.getValue(), timeout.getTimeUnit());
            if (!acquired) {
                throw MESSAGES.failToObtainLock(context, timeout.getValue(), timeout.getTimeUnit());
            }
            synchronized (threadLock) {
                if (ROOT_LOGGER.isTraceEnabled()) {
                    ROOT_LOGGER.trace("Acquired lock: " + lock + " for stateful component instance: " + instance + " during invocation: " + context);
                }

                Object currentTransactionKey = null;
                boolean wasTxSyncRegistered = false;
                try {
                    //we never register a sync for bean managed transactions
                    //the inner BMT interceptor is going to setup the correct transaction anyway
                    //so enrolling in an existing transaction is not correct
                    if(containerManagedTransactions) {
                        if (!instance.isSynchronizationRegistered()) {
                            // get the key to current transaction associated with this thread
                            currentTransactionKey = transactionSynchronizationRegistry.getTransactionKey();
                            final int status = transactionSynchronizationRegistry.getTransactionStatus();
                            // if this SFSB instance is already associated with a different transaction, then it's an error
                            // if the thread is currently associated with a tx, then register a tx synchronization
                            if (currentTransactionKey != null && status != Status.STATUS_COMMITTED) {
                                // register a tx synchronization for this SFSB instance
                                final Synchronization statefulSessionSync = new StatefulSessionSynchronization(instance, lockOwner);
                                transactionSynchronizationRegistry.registerInterposedSynchronization(statefulSessionSync);
                                wasTxSyncRegistered = true;
                                if (ROOT_LOGGER.isTraceEnabled()) {
                                    ROOT_LOGGER.trace("Registered tx synchronization: " + statefulSessionSync + " for tx: " + currentTransactionKey +
                                            " associated with stateful component instance: " + instance);
                                }
                                // invoke the afterBegin callback on the SFSB
                                instance.afterBegin();
                                instance.setSynchronizationRegistered(true);
                                context.putPrivateData(StatefulTransactionMarker.class, StatefulTransactionMarker.of(true));
                            }
                        } else {
                            context.putPrivateData(StatefulTransactionMarker.class, StatefulTransactionMarker.of(false));
                        }
                    }
                    // proceed with the invocation
                    return context.proceed();

                } finally {
                    // if the current call did *not* register a tx SessionSynchronization, then we have to explicitly mark the
                    // SFSB instance as "no longer in use". If it registered a tx SessionSynchronization, then releasing the lock is
                    // taken care off by a tx synchronization callbacks.
                    if (!wasTxSyncRegistered && !instance.isSynchronizationRegistered()) {
                        releaseInstance(instance);
                    } else if (!wasTxSyncRegistered) {
                        //if we don't release the lock here then it will be aquiared multiple times
                        //and only released once
                        releaseLock(instance);
                        //we also call the cache release to decrease the usage count
                        instance.getComponent().getCache().release(instance);
                    }
                }
            }
        } finally {
            lock.popOwner();
        }
    }

    /**
     * Use either the active transaction or the current thread as the lock owner
     *
     * @param transactionSynchronizationRegistry
     *         The synronization registry
     * @return The lock owner
     */
    private static Object getLockOwner(final TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
        Object owner = transactionSynchronizationRegistry.getTransactionKey();
        return owner != null ? owner : Thread.currentThread();
    }

    /**
     * Releases the passed {@link StatefulSessionComponentInstance} i.e. marks it as no longer in use. After releasing the
     * instance, this method releases the lock, held by this thread, on the stateful component instance.
     *
     * @param instance The stateful component instance
     */
    void releaseInstance(final StatefulSessionComponentInstance instance) {
        try {
            // mark the SFSB instance as no longer in use
            instance.getComponent().getCache().release(instance);
        } finally {
            instance.setSynchronizationRegistered(false);
            // release the lock on the SFSB instance
            this.releaseLock(instance);
        }
    }

    /**
     * Releases the lock, held by this thread, on the stateful component instance.
     */
    void releaseLock(final StatefulSessionComponentInstance instance) {
        instance.getLock().unlock();
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.tracef("Released lock: %s", instance.getLock());
        }
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


    private class StatefulSessionSynchronization implements Synchronization {

        private final StatefulSessionComponentInstance statefulSessionComponentInstance;
        private final Object lockOwner;

        StatefulSessionSynchronization(StatefulSessionComponentInstance statefulSessionComponentInstance, final Object lockOwner) {
            this.statefulSessionComponentInstance = statefulSessionComponentInstance;
            this.lockOwner = lockOwner;
        }

        @Override
        public void beforeCompletion() {
            try {
                if (ROOT_LOGGER.isTraceEnabled()) {
                    ROOT_LOGGER.trace("Before completion callback invoked on Transaction synchronization: " + this +
                            " of stateful component instance: " + statefulSessionComponentInstance);
                }
                if (!statefulSessionComponentInstance.isDiscarded()) {
                    statefulSessionComponentInstance.beforeCompletion();
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }

        @Override
        public void afterCompletion(int status) {
            try {
                if (ROOT_LOGGER.isTraceEnabled()) {
                    ROOT_LOGGER.trace("After completion callback invoked on Transaction synchronization: " + this +
                            " of stateful component instance: " + statefulSessionComponentInstance);
                }
                if (!statefulSessionComponentInstance.isDiscarded()) {
                    statefulSessionComponentInstance.afterCompletion(status == Status.STATUS_COMMITTED);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
            // tx has completed, so mark the SFSB instance as no longer in use
            statefulSessionComponentInstance.getLock().pushOwner(lockOwner);
            try {
                releaseInstance(statefulSessionComponentInstance);
            } finally {
                statefulSessionComponentInstance.getLock().popOwner();
            }
        }

        private void handleThrowable(Throwable t) {
            ROOT_LOGGER.discardingStatefulComponent(statefulSessionComponentInstance, t);
            try {
                // discard the SFSB instance
                statefulSessionComponentInstance.discard();
            } finally {
                statefulSessionComponentInstance.getLock().pushOwner(lockOwner);
                try {
                    // release the lock associated with the SFSB instance
                    releaseLock(statefulSessionComponentInstance);
                } finally {
                    statefulSessionComponentInstance.getLock().popOwner();
                }
            }
            // throw back an appropriate exception
            if (t instanceof RuntimeException)
                throw (RuntimeException) t;
            if (t instanceof Error)
                throw (Error) t;
            throw (EJBException) new EJBException().initCause(t);
        }
    }

}
