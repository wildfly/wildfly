/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.bmt.timeout;

import java.rmi.RemoteException;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;
import org.jboss.logging.Logger;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class StatelessBmtBean {
    private static Logger log = Logger.getLogger(StatelessBmtBean.class);

    @Resource
    private UserTransaction txn;

    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @Inject
    private TransactionCheckerSingleton checker;

    public String testTransaction(int transactionCount, int timeoutCount)
            throws RemoteException, NamingException, SystemException {
        TxTestUtil.checkTransactionExists(tm, false);

        for(int i = 1; i <= transactionCount; i++) {
            boolean isTimeout = (i <= timeoutCount);
            runTransaction(isTimeout);
        }

        return TxTestUtil.getStatusAsString(txn.getStatus());
    }

    private void runTransaction(boolean isTimeout) {
        String txnToString = null;

        try {
            if(isTimeout) {
                txn.setTransactionTimeout(1);
            }

            txn.begin();
            TxTestUtil.checkTransactionExists(tm, true);

            txnToString = txn.toString();

            TxTestUtil.enlistTestXAResource(tm.getTransaction(), checker);
            TxTestUtil.enlistTestXAResource(tm.getTransaction(), checker);

            if(isTimeout) {
                TxTestUtil.waitForTimeout(tm);
            }

            log.tracef("Commiting transaction '%s'", txnToString);
            txn.commit();

            if(isTimeout) {
                throw new RuntimeException("Expecting transaction being timeouted but it's in state '"
                    + TxTestUtil.getStatusAsString(txn.getStatus()) + "'");
            }
        } catch (RollbackException e) {
            log.tracef("Rollbacking transaction '%s'.", txnToString);
            try {
                txn.rollback();
            } catch (Exception rollbacke) {
                log.debugf(rollbacke, "Expected transaction as can't rollback non active - TM aborted transaction");
            }
            if(isTimeout) {
                log.tracef("Transaction '%s' was timeouted as expected", txnToString);
            } else {
                throw new RuntimeException("Transaction should not be rollbacked as wasn't timeouted", e);
            }
        } catch (Exception e) {
            // Heuristic exceptions handled here
            throw new RuntimeException("Not expected exception which fails the test", e);
        } finally {
            try {
                txn.setTransactionTimeout(0);
            } catch (SystemException se) {
                throw new RuntimeException("Can't reset transaction timeout", se);
            }
        }
    }
}
