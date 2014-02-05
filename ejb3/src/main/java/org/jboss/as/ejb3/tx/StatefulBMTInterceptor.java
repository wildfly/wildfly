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

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.invocation.InterceptorContext;

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
public class StatefulBMTInterceptor extends BMTInterceptor {

    public StatefulBMTInterceptor(final EJBComponent component) {
        super(component);
    }

    private void checkBadStateful() {
        int status = Status.STATUS_NO_TRANSACTION;
        TransactionManager tm = getComponent().getTransactionManager();
        try {
            status = tm.getStatus();
        } catch (SystemException ex) {
            EjbLogger.ROOT_LOGGER.failedToGetStatus(ex);
        }

        switch (status) {
            case Status.STATUS_COMMITTING:
            case Status.STATUS_MARKED_ROLLBACK:
            case Status.STATUS_PREPARING:
            case Status.STATUS_ROLLING_BACK:
                try {
                    tm.rollback();
                } catch (Exception ex) {
                    EjbLogger.ROOT_LOGGER.failedToRollback(ex);
                }
                EjbLogger.ROOT_LOGGER.transactionNotComplete(getComponent().getComponentName(), statusAsString(status));
        }
    }

    @Override
    protected Object handleInvocation(final InterceptorContext invocation) throws Exception {
        final StatefulSessionComponentInstance instance = (StatefulSessionComponentInstance) invocation.getPrivateData(ComponentInstance.class);

        TransactionManager tm = getComponent().getTransactionManager();
        assert tm.getTransaction() == null : "can't handle BMT transaction, there is a transaction active";

        // Is the instance already associated with a transaction?
        Transaction tx = instance.getTransaction();
        if (tx != null) {
            // then resume that transaction.
            instance.setTransaction(null);
            tm.resume(tx);
        }
        try {
            return invocation.proceed();
        } catch (Throwable e) {
            throw this.handleException(invocation, e);
        } finally {
            checkBadStateful();
            // Is the instance finished with the transaction?
            Transaction newTx = tm.getTransaction();
            //always set it, even if null
            instance.setTransaction(newTx);
            if (newTx != null) {
                // remember the association
                // and suspend it.
                tm.suspend();
            }
        }
    }
}
