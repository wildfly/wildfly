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

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.RollbackException;
import javax.transaction.TransactionManager;

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
