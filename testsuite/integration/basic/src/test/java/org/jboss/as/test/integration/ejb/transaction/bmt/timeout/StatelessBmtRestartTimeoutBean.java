/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.bmt.timeout;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.transaction.RollbackException;
import jakarta.transaction.TransactionManager;

import org.jboss.as.test.integration.transactions.TxTestUtil;
import org.jboss.logging.Logger;
import org.junit.Assert;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class StatelessBmtRestartTimeoutBean {
    private static Logger log = Logger.getLogger(StatelessBmtRestartTimeoutBean.class);

    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    public void test() throws Exception {
        String txnAsString = null;

        tm.setTransactionTimeout(1);

        try {
            tm.begin();
            txnAsString = tm.getTransaction().toString();
            TxTestUtil.waitForTimeout(tm);
            tm.commit();
        } catch (RollbackException e) {
            try {
                log.tracef("Rollbacking transaction '%s'", txnAsString);
                tm.rollback();
                Assert.fail("Rollback should throw IllegalStateException: BaseTransaction.rollback - ARJUNA016074: no transaction!"
                    + " as the original transaction should be aborted by transaction manager");
            } catch (Exception rollbacke) {
                // expected transaction as there is no txn to rollback - it was aborted by TM
                log.debugf(rollbacke, "Got expected transaction: %s'", rollbacke.getClass());
            }
        } finally {
            // reseting transaction timeout to default one
            tm.setTransactionTimeout(0);
        }

        // after reset additional check if default value is used
        // value depends on settings under txn subsystem but default is 5 minutes
        tm.begin();
        txnAsString = tm.getTransaction().toString();
        TxTestUtil.waitForTimeout(tm);
        tm.commit();
    }
}
