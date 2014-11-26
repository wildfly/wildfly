/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.iiop;

import javax.ejb.TransactionAttributeType;
import javax.transaction.Transaction;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
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
            MethodIntf methodIntf = invocation.getPrivateData(MethodIntf.class);
            if (methodIntf == null) {
                final ComponentView componentView = invocation.getPrivateData(ComponentView.class);
                if (componentView != null) {
                    methodIntf = componentView.getPrivateData(MethodIntf.class);
                } else {
                    methodIntf = MethodIntf.BEAN;
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
