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

import org.jboss.ejb3.context.spi.InvocationContext;
import org.jboss.ejb3.tx2.spi.TransactionalInvocationContext;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * CMT interceptor for timer invocations. An exception is thrown if the transaction is rolled back, so the timer
 * service knows to retry the timeout.
 *
 * @author Stuart Douglas
 */
public class TimerCMTTxInterceptor extends org.jboss.ejb3.tx2.impl.CMTTxInterceptor implements Interceptor {
    @Override
    public Object processInvocation(InterceptorContext invocation) throws Exception {
        return super.invoke((TransactionalInvocationContext) invocation.getPrivateData(InvocationContext.class));
    }

    @Override
    protected void endTransaction(final TransactionManager tm, final Transaction tx) {
        boolean rolledBack = false;
        try {
            if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                rolledBack = true;
            }
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
        super.endTransaction(tm, tx);
        if (rolledBack) {
            throw new TimerTransactionRolledBackException("Timer invocation failed, transaction rolled back");
        }
    }

    private Object processInvocation(TransactionalInvocationContext invocation) throws Exception {
        return requiresNew(invocation);
    }

}
