/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.iiop;

import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.Transaction;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;
import org.wildfly.iiop.openjdk.tm.ForeignTransaction;
import org.wildfly.iiop.openjdk.tm.TxServerInterceptor;

/**
 * @author Stuart Douglas
 */
public class EjbIIOPTransactionInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new EjbIIOPTransactionInterceptor());

    @Override
    public Object processInvocation(final InterceptorContext invocation) throws Exception {
        // Do we have a foreign transaction context?
        Transaction tx = TxServerInterceptor.getCurrentTransaction();
        if (tx instanceof ForeignTransaction) {
            final EJBComponent component = (EJBComponent) invocation.getPrivateData(Component.class);

            //for timer invocations there is no view, so the methodInf is attached directly
            //to the context. Otherwise we retrieve it from the invoked view
            MethodInterfaceType methodIntf = invocation.getPrivateData(MethodInterfaceType.class);
            if (methodIntf == null) {
                final ComponentView componentView = invocation.getPrivateData(ComponentView.class);
                if (componentView != null) {
                    methodIntf = componentView.getPrivateData(MethodInterfaceType.class);
                } else {
                    methodIntf = MethodInterfaceType.Bean;
                }
            }

            final TransactionAttributeType attr = component.getTransactionAttributeType(methodIntf, invocation.getMethod());
            if (attr != TransactionAttributeType.NOT_SUPPORTED && attr != TransactionAttributeType.REQUIRES_NEW) {
                throw EjbLogger.ROOT_LOGGER.transactionPropagationNotSupported();
            }
        }
        return invocation.proceed();
    }
}
