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
package org.jboss.as.ejb3.component.entity.interceptors;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentInstanceInterceptorFactory;
import org.jboss.as.ejb3.component.AbstractEJBInterceptor;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.as.ejb3.tx.OwnableReentrantLock;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.logging.Logger;

/**
 * {@link org.jboss.invocation.Interceptor} which manages {@link javax.transaction.Synchronization} semantics on an entity bean.
 * <p/>
 * For now we are using a completely synchronized approach to entity concurrency. There is at most 1 entity active for a given primary
 * key at any time, and access is synchronized within a transaction.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Jaikiran Pai
 * @author Stuart Douglas
 */
public class EntityBeanSynchronizationInterceptor extends AbstractEJBInterceptor {
    private static final Logger log = Logger.getLogger(EntityBeanSynchronizationInterceptor.class);

    private final Object threadLock = new Object();
    private final OwnableReentrantLock lock = new OwnableReentrantLock();


    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final EntityBeanComponent component = getComponent(context, EntityBeanComponent.class);
        final EntityBeanComponentInstance instance = (EntityBeanComponentInstance) context.getPrivateData(ComponentInstance.class);

        //we do not synchronize for instances that are not associated with an identity
        if (instance.getPrimaryKey() == null) {
            return context.proceed();
        }

        final TransactionSynchronizationRegistry transactionSynchronizationRegistry = component.getTransactionSynchronizationRegistry();
        if (log.isTraceEnabled()) {
            log.trace("Trying to acquire lock: " + lock + " for entity bean " + instance + " during invocation: " + context);
        }
        lock.pushOwner(getLockOwner(transactionSynchronizationRegistry));
        try {
            // we obtain a lock in this synchronization interceptor because the lock needs to be tied to the synchronization
            // so that it can released on the tx synchronization callbacks
            lock.lock();
            synchronized (lock) {
                if (log.isTraceEnabled()) {
                    log.trace("Acquired lock: " + lock + " for entity bean instance: " + instance + " during invocation: " + context);
                }

                Object currentTransactionKey = null;
                boolean wasTxSyncRegistered = false;
                try {
                    // get the key to current transaction associated with this thread
                    currentTransactionKey = transactionSynchronizationRegistry.getTransactionKey();
                    // if this entity instance is already associated with a different transaction, then it's an error

                    // if the thread is currently associated with a tx, then register a tx synchronization
                    if (currentTransactionKey != null) {
                        // register a tx synchronization for this entity instance
                        final Synchronization statefulSessionSync = new EntityBeanSynchronization(instance);
                        transactionSynchronizationRegistry.registerInterposedSynchronization(statefulSessionSync);
                        wasTxSyncRegistered = true;
                        if (log.isTraceEnabled()) {
                            log.trace("Registered tx synchronization: " + statefulSessionSync + " for tx: " + currentTransactionKey +
                                    " associated with stateful component instance: " + instance);
                        }
                    }
                    // proceed with the invocation
                    return context.proceed();

                } finally {
                    // if the current call did *not* register a tx SessionSynchronization, then we have to explicitly mark the
                    // SFSB instance as "no longer in use". If it registered a tx SessionSynchronization, then releasing the lock is
                    // taken care off by a tx synchronization callbacks.
                    if (!wasTxSyncRegistered) {
                        instance.store();
                        releaseInstance(instance, true);
                    }
                }
            }
        } finally {
            lock.popOwner();

        }
    }

    /**
     * Releases the passed {@link EntityBeanComponentInstance} i.e. marks it as no longer in use. After releasing the
     * instance, this method releases the lock, held by this thread, on the stateful component instance.
     *
     * @param instance The stateful component instance
     * @param success
     */
    private void releaseInstance(final EntityBeanComponentInstance instance, final boolean success) {
        try {
            // mark the SFSB instance as no longer in use
            instance.getComponent().getCache().release(instance, success);
        } finally {
            // release the lock on the SFSB instance
            this.releaseLock();
        }
    }

    /**
     * Releases the lock, held by this thread, on the stateful component instance.
     */
    private void releaseLock() {
        lock.unlock();
        if (log.isTraceEnabled()) {
            log.trace("Released lock: " + lock);
        }
    }

    private class EntityBeanSynchronization implements Synchronization {

        private final EntityBeanComponentInstance componentInstance;

        EntityBeanSynchronization(EntityBeanComponentInstance componentInstance) {
            this.componentInstance = componentInstance;
        }

        @Override
        public void beforeCompletion() {
            synchronized (threadLock) {
                //invoke the EJB store method within the transaction
                componentInstance.store();
            }
        }

        @Override
        public void afterCompletion(int status) {
            synchronized (threadLock) {

                lock.pushOwner(getLockOwner(componentInstance.getComponent().getTransactionSynchronizationRegistry()));
                try {
                // tx has completed, so mark the SFSB instance as no longer in use
                releaseInstance(componentInstance, status == Status.STATUS_COMMITTED);
                } finally {
                    lock.popOwner();
                }
            }
        }

    }

    public static final InterceptorFactory FACTORY = new ComponentInstanceInterceptorFactory() {

        @Override
        protected Interceptor create(final Component component, final InterceptorFactoryContext context) {
            return new EntityBeanSynchronizationInterceptor();
        }
    };


    /**
     * Use either the active transaction or the current thread as the lock owner
     *
     * @param transactionSynchronizationRegistry
     *         The synronization registry
     * @return The lock owner
     */
    private Object getLockOwner(final TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
        Object owner = transactionSynchronizationRegistry.getTransactionKey();
        return owner != null ? owner : Thread.currentThread();
    }


}
