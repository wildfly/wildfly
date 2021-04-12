/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
