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
package org.jboss.as.ejb3.tx;

import javax.ejb.TransactionAttributeType;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.LocalTransaction;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * Transaction interceptor for Singleton and Stateless beans,
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class LifecycleCMTTxInterceptor extends CMTTxInterceptor {

    private final TransactionAttributeType transactionAttributeType;
    private final int transactionTimeout;

    public LifecycleCMTTxInterceptor(final TransactionAttributeType transactionAttributeType, final int transactionTimeout) {
        this.transactionAttributeType = transactionAttributeType;
        this.transactionTimeout = transactionTimeout;
    }

    @Override
    public Object processInvocation(InterceptorContext invocation) throws Exception {
        final EJBComponent component = (EJBComponent) invocation.getPrivateData(Component.class);

        switch (transactionAttributeType) {
            case MANDATORY:
                return mandatory(invocation, component);
            case NEVER:
                return never(invocation, component);
            case NOT_SUPPORTED:
                return notSupported(invocation, component);
            case REQUIRED:
                return required(invocation, component, transactionTimeout);
            case REQUIRES_NEW:
                return requiresNew(invocation, component, transactionTimeout);
            case SUPPORTS:
                return supports(invocation, component);
            default:
                throw EjbLogger.ROOT_LOGGER.unknownTxAttributeOnInvocation(transactionAttributeType, invocation);
        }
    }

    protected Transaction beginTransaction(final TransactionManager tm) throws NotSupportedException, SystemException {
        if (tm instanceof ContextTransactionManager) {
            final ContextTransactionManager contextTransactionManager = (ContextTransactionManager) tm;
            int timeout = contextTransactionManager.getTransactionTimeout();
            final LocalTransaction transaction = LocalTransactionContext.getCurrent().beginTransaction(timeout, false);
            try {
                contextTransactionManager.resume(transaction);
            } catch (InvalidTransactionException e) {
                // should not be possible
                throw new IllegalStateException(e);
            }
            return transaction;
        } else {
            return super.beginTransaction(tm);
        }
    }

    @Override
    protected Object notSupported(InterceptorContext invocation, EJBComponent component) throws Exception {
        TransactionManager tm = component.getTransactionManager();
        Transaction tx = tm.getTransaction();
        int status = (tx != null) ? tx.getStatus() : Status.STATUS_NO_TRANSACTION;
        // If invocation was triggered from Synchronization.afterCompletion(...)
        // then skip suspend/resume of associated tx since JTS refuses to resume a completed tx
        switch (status) {
            case Status.STATUS_NO_TRANSACTION:
            case Status.STATUS_COMMITTED:
            case Status.STATUS_ROLLEDBACK: {
                return this.invokeInNoTx(invocation, component);
            }
            default: {
                Transaction suspendedTx = tm.suspend();
                try {
                    return this.invokeInNoTx(invocation, component);
                } finally {
                    if (suspendedTx != null) {
                        tm.resume(suspendedTx);
                    }
                }
            }
        }
    }

    /**
     * @author Stuart Douglas
     */
    public static class Factory extends ComponentInterceptorFactory {

        private final MethodIdentifier methodIdentifier;

        public Factory(final MethodIdentifier methodIdentifier) {
            this.methodIdentifier = methodIdentifier;
        }

        @Override
        protected Interceptor create(Component component, InterceptorFactoryContext context) {
            final EJBComponent ejb = (EJBComponent) component;
            TransactionAttributeType txAttr;
            if (methodIdentifier == null) {
                txAttr = TransactionAttributeType.REQUIRED;
            } else {
                txAttr = ejb.getTransactionAttributeType(MethodIntf.BEAN, methodIdentifier, TransactionAttributeType.REQUIRED);
            }
            final int txTimeout;
            if(methodIdentifier == null) {
                txTimeout = -1;
            } else {
                txTimeout = ejb.getTransactionTimeout(MethodIntf.BEAN, methodIdentifier);
            }
            if (txAttr == TransactionAttributeType.REQUIRED) {
                txAttr = TransactionAttributeType.REQUIRES_NEW;
            }
            final LifecycleCMTTxInterceptor interceptor = new LifecycleCMTTxInterceptor(txAttr, txTimeout);
            return interceptor;
        }
    }

}
