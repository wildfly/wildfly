/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import org.jboss.logging.Logger;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import static org.jboss.as.ejb3.tx.util.StatusHelper.statusAsString;

/**
 * A per instance interceptor that keeps an association with the outcoming transaction.
 * <p/>
 * EJB 3 13.6.1:
 * In the case of a stateful session bean, it is possible that the business method that started a transaction
 * completes without committing or rolling back the transaction. In such a case, the container must retain
 * the association between the transaction and the instance across multiple client calls until the instance
 * commits or rolls back the transaction. When the client invokes the next business method, the container
 * must invoke the business method (and any applicable interceptor methods for the bean) in this transac-
 * tion context.
 *
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public abstract class StatefulBMTInterceptor extends BMTInterceptor {
    private static final Logger log = Logger.getLogger(StatefulBMTInterceptor.class);

    /**
     * The transaction associated with the current instance.
     */
    private volatile Transaction transaction;

    private void checkBadStateful() {
        int status = Status.STATUS_NO_TRANSACTION;
        TransactionManager tm = this.getTransactionManager();
        try {
            status = tm.getStatus();
        } catch (SystemException ex) {
            log.error("Failed to get status", ex);
        }

        switch (status) {
            case Status.STATUS_COMMITTING:
            case Status.STATUS_MARKED_ROLLBACK:
            case Status.STATUS_PREPARING:
            case Status.STATUS_ROLLING_BACK:
                try {
                    tm.rollback();
                } catch (Exception ex) {
                    log.error("Failed to rollback", ex);
                }
                String msg = "BMT stateful bean '" + getComponentName()
                        + "' did not complete user transaction properly status=" + statusAsString(status);
                log.error(msg);
        }
    }

    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    protected Object handleInvocation(TransactionalInvocationContext invocation) throws Exception {
        TransactionManager tm = this.getTransactionManager();
        assert tm.getTransaction() == null : "can't handle BMT transaction, there is a transaction active";

        // Is the instance already associated with a transaction?
        Transaction tx = transaction;
        if (tx != null) {
            transaction = null;
            // then resume that transaction.
            tm.resume(tx);
        }
        try {
            return invocation.proceed();
        } catch (Exception e) {
            throw this.handleException(invocation, e);
        } finally {
            checkBadStateful();
            // Is the instance finished with the transaction?
            Transaction newTx = tm.getTransaction();
            if (newTx != null) {
                // remember the association
                transaction = newTx;
                // and suspend it.
                tm.suspend();
            } else {
                // forget any previous associated transaction
                transaction = null;
            }
        }
    }
}
