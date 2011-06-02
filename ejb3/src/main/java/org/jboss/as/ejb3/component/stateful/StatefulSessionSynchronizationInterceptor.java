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

import org.jboss.as.ejb3.component.AbstractEJBInterceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.EJBException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;
import java.util.concurrent.locks.ReentrantLock;

import static org.jboss.as.ejb3.component.stateful.StatefulComponentInstanceInterceptor.getComponentInstance;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatefulSessionSynchronizationInterceptor extends AbstractEJBInterceptor {
    private static final Logger log = Logger.getLogger(StatefulSessionSynchronizationInterceptor.class);

    private final ReentrantLock lock = new ReentrantLock(true);
    private Object transactionKey = null;

    private static Error handleThrowable(final Throwable t) {
        log.error(t);
        if (t instanceof RuntimeException)
            throw (RuntimeException) t;
        if (t instanceof Error)
            throw (Error) t;
        throw (EJBException) new EJBException().initCause(t);
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final StatefulSessionComponent component = getComponent(context, StatefulSessionComponent.class);
        final StatefulSessionComponentInstance instance = getComponentInstance(context);

        AccessTimeout timeout = component.getAccessTimeout(context.getMethod());
        boolean acquired = lock.tryLock(timeout.value(), timeout.unit());
        if(!acquired) {
            throw new ConcurrentAccessTimeoutException("EJB 3.1 FR 4.3.14.1 concurrent access timeout on " + context
                 + " - could not obtain lock within " + timeout.value() + timeout.unit());
        }

        TransactionSynchronizationRegistry transactionSynchronizationRegistry = component.getTransactionSynchronizationRegistry();
        Object currentTransactionKey = transactionSynchronizationRegistry.getTransactionKey();
        if(transactionKey != null) {
            if(!transactionKey.equals(currentTransactionKey))
                throw new EJBException("EJB 3.1 FR 4.6 Stateful instance " + instance + " is already associated with tx " + transactionKey + " (current tx " + currentTransactionKey + ")");
        } else {
            if(currentTransactionKey != null) {
                transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
                    @Override
                    public void beforeCompletion() {
                        try {
                            instance.beforeCompletion();
                        } catch (Throwable t) {
                            instance.discard();
                            throw handleThrowable(t);
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        try {
                            instance.afterCompletion(status == Status.STATUS_COMMITTED);
                            release(instance);
                        } catch (Throwable t) {
                            instance.discard();
                            throw handleThrowable(t);
                        }
                    }
                });
                instance.afterBegin();
                transactionKey = currentTransactionKey;
            }
        }
        try {
            return context.proceed();
        }
        finally {
            if(currentTransactionKey == null)
                release(instance);
        }
    }

    private void release(final StatefulSessionComponentInstance instance) {
        // TODO: remove
        instance.getComponent().getCache().release(instance);
        transactionKey = null;
        lock.unlock();
    }
}
