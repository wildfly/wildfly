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

package org.jboss.as.ejb3.component.stateful;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;
import org.jboss.tm.TxUtils;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import java.io.Serializable;

/**
 * An interceptor which handles a invocation on a {@link javax.ejb.Remove} method of a stateful session bean. This interceptor
 * removes the stateful session once the method completes (either successfully or with an exception). If the remove method
 * was marked with "retainIfException" to true and if the remove method threw a {@link javax.ejb.ApplicationException} then
 * this interceptor does *not* remove the stateful session.
 * <p/>
 * User: Jaikiran Pai
 */
public class StatefulRemoveInterceptor implements Interceptor {

    private static final Logger logger = Logger.getLogger(StatefulRemoveInterceptor.class);

    private final boolean retainIfException;

    public StatefulRemoveInterceptor(final boolean retainIfException) {
        this.retainIfException = retainIfException;
    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final Component component = context.getPrivateData(Component.class);
        // just log a WARN and throw back the original exception
        if (component instanceof StatefulSessionComponent == false) {
            throw new RuntimeException("Unexpected component: " + component + " in interceptor context: " + context +
                    " Expected an instance of " + StatefulSessionComponent.class);
        }
        final StatefulSessionComponent statefulComponent = (StatefulSessionComponent) component;
        Object invocationResult = null;
        try {
            // proceed
            invocationResult = context.proceed();
        } catch (Exception e) {
            // Exception caught during call to @Remove method. Handle it appropriately

            // If it's an application exception and if the @Remove method has set "retainIfException" to true
            // then just throw back the exception and don't remove the session instance.
            if (this.isApplicationException(statefulComponent, e.getClass()) && this.retainIfException) {
                throw e;
            }
            // otherwise, just remove it and throw back the original exception
            final StatefulSessionComponentInstance statefulComponentInstance = (StatefulSessionComponentInstance) context.getPrivateData(ComponentInstance.class);
            final Serializable sessionId = statefulComponentInstance.getId();
            removeSession(statefulComponent, sessionId);
            throw e;
        }
        final StatefulSessionComponentInstance statefulComponentInstance = (StatefulSessionComponentInstance) context.getPrivateData(ComponentInstance.class);
        final Serializable sessionId = statefulComponentInstance.getId();
        // just remove the session because of a call to @Remove method
        this.removeSession(statefulComponent, sessionId);
        // return the invocation result
        return invocationResult;
    }

    /**
     * Removes the session associated with the <code>sessionId</code>.
     *
     * @param statefulComponent The stateful component
     * @param sessionId         The session id
     */
    private void removeSession(final StatefulSessionComponent statefulComponent, final Serializable sessionId) {
        Transaction currentTx = null;
        try {
            currentTx = statefulComponent.getTransactionManager().getTransaction();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }

        if (currentTx != null && TxUtils.isActive(currentTx)) {
            try {
                // A transaction is in progress, so register a Synchronization so that the session can be removed on tx
                // completion.
                currentTx.registerSynchronization(new RemoveSynchronization(statefulComponent, sessionId));
            } catch (RollbackException e) {
                throw new RuntimeException(e);
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
        } else {
            // no tx currently in progress, so just remove the session
            statefulComponent.getCache().remove(sessionId);
        }

    }

    /**
     * Returns true if the passed <code>exceptionClass</code> is an application exception. Else returns false.
     *
     * @param ejbComponent   The EJB component
     * @param exceptionClass The exception class
     * @return
     */
    private boolean isApplicationException(final EJBComponent ejbComponent, final Class<?> exceptionClass) {
        return ejbComponent.getApplicationException(exceptionClass) != null;
    }

    /**
     * A {@link Synchronization} which removes a stateful session in it's {@link Synchronization#afterCompletion(int)}
     * callback.
     */
    private static class RemoveSynchronization implements Synchronization {
        private final StatefulSessionComponent statefulComponent;
        private final Serializable sessionId;

        public RemoveSynchronization(final StatefulSessionComponent component, final Serializable sessionId) {
            if (sessionId == null) {
                throw new IllegalArgumentException("Session id cannot be null");
            }
            if (component == null) {
                throw new IllegalArgumentException("Stateful component cannot be null");
            }
            this.sessionId = sessionId;
            this.statefulComponent = component;

        }

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            try {
                // remove the session
                this.statefulComponent.getCache().remove(this.sessionId);
            } catch (Throwable t) {
                // An exception thrown from afterCompletion is gobbled up
                logger.error("Failed to remove bean: " + this.statefulComponent.getComponentName() + " with session id " + this.sessionId, t);
                if (t instanceof Error)
                    throw (Error) t;
                if (t instanceof RuntimeException)
                    throw (RuntimeException) t;
                throw new RuntimeException(t);
            }
        }
    }
}
