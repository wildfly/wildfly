/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.tx;

import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.Status;
import jakarta.transaction.Transaction;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;
import org.wildfly.transaction.client.ContextTransactionManager;

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

    @Override
    protected Object notSupported(InterceptorContext invocation, EJBComponent component) throws Exception {
        Transaction tx = ContextTransactionManager.getInstance().getTransaction();
        int status = (tx != null) ? tx.getStatus() : Status.STATUS_NO_TRANSACTION;
        // If invocation was triggered from Synchronization.afterCompletion(...)
        // then skip suspend/resume of associated tx since JTS refuses to resume a completed tx
        switch (status) {
            case Status.STATUS_NO_TRANSACTION:
            case Status.STATUS_COMMITTED:
            case Status.STATUS_ROLLEDBACK: {
                return invokeInNoTx(invocation, component);
            }
            default: {
                return super.notSupported(invocation, component);
            }
        }
    }

    /**
     * @author Stuart Douglas
     */
    public static class Factory extends ComponentInterceptorFactory {

        private final MethodIdentifier methodIdentifier;
        /**
         * If this is false it means the bean is a SFSB, and can only be NOT_SUPPORTED or REQUIRES_NEW
         */
        private final boolean treatRequiredAsRequiresNew;

        public Factory(final MethodIdentifier methodIdentifier, final boolean treatRequiredAsRequiresNew) {
            this.methodIdentifier = methodIdentifier;
            this.treatRequiredAsRequiresNew = treatRequiredAsRequiresNew;
        }

        @Override
        protected Interceptor create(Component component, InterceptorFactoryContext context) {
            final EJBComponent ejb = (EJBComponent) component;
            TransactionAttributeType txAttr;
            if (methodIdentifier == null) {
                if(treatRequiredAsRequiresNew) {
                    txAttr = TransactionAttributeType.REQUIRED;
                } else {
                    //for stateful beans we default to NOT_SUPPORTED
                    txAttr = TransactionAttributeType.NOT_SUPPORTED;
                }
            } else {
                txAttr = ejb.getTransactionAttributeType(MethodInterfaceType.Bean, methodIdentifier, treatRequiredAsRequiresNew ? TransactionAttributeType.REQUIRES_NEW : TransactionAttributeType.NOT_SUPPORTED);
            }
            final int txTimeout;
            if(methodIdentifier == null) {
                txTimeout = -1;
            } else {
                txTimeout = ejb.getTransactionTimeout(MethodInterfaceType.Bean, methodIdentifier);
            }
            if (treatRequiredAsRequiresNew && txAttr == TransactionAttributeType.REQUIRED) {
                txAttr = TransactionAttributeType.REQUIRES_NEW;
            }
            if (!treatRequiredAsRequiresNew
                    && txAttr != TransactionAttributeType.NOT_SUPPORTED
                    && txAttr != TransactionAttributeType.REQUIRES_NEW) {
                if (ejb.isTransactionAttributeTypeExplicit(MethodInterfaceType.Bean, methodIdentifier)) {
                    EjbLogger.ROOT_LOGGER.invalidTransactionTypeForSfsbLifecycleMethod(txAttr, methodIdentifier,
                            ejb.getComponentClass());
                }
                txAttr = TransactionAttributeType.NOT_SUPPORTED;
            }
            return new LifecycleCMTTxInterceptor(txAttr, txTimeout);
        }
    }

}
