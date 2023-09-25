/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.bmt.timeout;

import java.rmi.RemoteException;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.inject.Inject;
import javax.naming.NamingException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
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
