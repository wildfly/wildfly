/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote.protocol.versionone;

import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinateTransaction;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinationManager;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.remoting3.Channel;

import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

/**
 * @author Jaikiran Pai
 */
class XidTransactionForgetTask extends XidTransactionManagementTask {

    XidTransactionForgetTask(final TransactionRequestHandler txRequestHandler, final EJBRemoteTransactionsRepository transactionsRepository,
                             final MarshallerFactory marshallerFactory, final XidTransactionID xidTransactionID,
                             final Channel channel, final short invocationId) {
        super(txRequestHandler, transactionsRepository, marshallerFactory, xidTransactionID, channel, invocationId);
    }

    @Override
    protected void manageTransaction() throws Throwable {
        // first associate the tx on this thread, by resuming the tx
        final Transaction transaction = this.transactionsRepository.removeTransaction(this.xidTransactionID);
        this.resumeTransaction(transaction);

        // "forget"
        final Xid xid = this.xidTransactionID.getXid();
        try {
            // get the subordinate tx
            final SubordinateTransaction subordinateTransaction = SubordinationManager.getTransactionImporter().getImportedTransaction(xid);

            if (subordinateTransaction == null) {
                throw new XAException(XAException.XAER_INVAL);
            }
            // invoke forget
            subordinateTransaction.doForget();

        } catch (Exception ex) {
            final XAException xaException = new XAException(XAException.XAER_RMERR);
            xaException.initCause(ex);
            throw xaException;

        } finally {
            SubordinationManager.getTransactionImporter().removeImportedTransaction(xid);
            // disassociate the tx that was asssociated (resumed) on this thread.
            // This needs to be done explicitly because the SubOrdinationManager isn't responsible
            // for clearing the tx context from the thread
            this.transactionsRepository.getTransactionManager().suspend();

        }
    }
}
