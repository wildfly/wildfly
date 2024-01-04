/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.outbound.connection.transaction.preparehalt;

import org.jboss.as.test.integration.transactions.PersistentTestXAResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

public class TestCommitFailureXAResource extends PersistentTestXAResource {
    private volatile boolean wasCommitted = false;

    public TestCommitFailureXAResource(TransactionCheckerSingleton checker) {
        super(checker);
    }

    /**
     * On first attemp to commit throwing {@link XAException#XAER_RMFAIL}
     * which represents an intermittent failure which the system could be recovered from later.
     * The transaction manager is expected to retry the commit.
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        if(!wasCommitted) {
            wasCommitted = true;
            throw new XAException(XAException.XAER_RMFAIL);
        } else {
            super.commit(xid, onePhase);
        }
    }
}
