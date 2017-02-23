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

package org.jboss.as.ejb3.remote;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.interceptors.AbstractEJBInterceptor;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.InterceptorContext;

/**
 * An interceptor which is responsible for identifying any remote transaction associated with the invocation
 * and propagating that transaction during the remaining part of the invocation
 *
 * @author Jaikiran Pai
 * @author Flavia Rainone
 * @deprecated Remove this class once WFLY-7860 is resolved.
 */
@Deprecated
class EJBRemoteTransactionPropagatingInterceptor extends AbstractEJBInterceptor {

    /**
     * Remote transactions repository
     */
    private final TransactionManager transactionManager;

    EJBRemoteTransactionPropagatingInterceptor(final TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Processes an incoming invocation and checks for the presence of a remote transaction associated with the
     * invocation context.
     *
     * @param context The invocation context
     * @return the invocation result
     * @throws Exception if the invocation or transaction creation fails
     */
    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final TransactionManager transactionManager = this.transactionManager;
        if (context.hasTransaction()) {
            // TODO: WFLY-7860
            try {
                transactionManager.resume(context.getTransaction());
            } catch (SystemException e) {
                try {
                    EJBComponent component = getComponent(context, EJBComponent.class);
                    // SystemException + server suspended equals the transaction is new and the request
                    // for new transaction is being rejected
                    if (component != null && component.getEjbSuspendHandlerService().isSuspended()) {
                        throw EjbLogger.ROOT_LOGGER.cannotBeginUserTransaction();
                    }
                } catch (RuntimeException unexpected) {
                    // just log, probably some other problem in the server is causing the original system
                    // exception and we don't want to overwrite the original exception
                    EjbLogger.ROOT_LOGGER.debug("Unexpected", unexpected);
                }
                throw e;
            }
            try {
                return context.proceed();
            } finally {
                transactionManager.suspend();
            }
        } else {
            return context.proceed();
        }
    }
}
