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

import com.arjuna.ats.arjuna.coordinator.TwoPhaseOutcome;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinateTransaction;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinationManager;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.logging.Logger;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.remoting3.Channel;
import org.xnio.IoUtils;

import javax.transaction.HeuristicCommitException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.IOException;

/**
 * @author Jaikiran Pai
 */
class XidTransactionPrepareTask extends XidTransactionManagementTask {

    private static final Logger logger = Logger.getLogger(XidTransactionPrepareTask.class);

    XidTransactionPrepareTask(final TransactionRequestHandler txRequestHandler, final EJBRemoteTransactionsRepository transactionsRepository,
                              final MarshallerFactory marshallerFactory, final XidTransactionID xidTransactionID,
                              final Channel channel, final short invocationId) {

        super(txRequestHandler, transactionsRepository, marshallerFactory, xidTransactionID, channel, invocationId);
    }

    @Override
    public void run() {
        try {
            this.manageTransaction();
        } catch (Throwable t) {
            try {
                // write out a failure message to the channel to let the client know that
                // the transaction operation failed
                transactionRequestHandler.writeException(this.channel, this.marshallerFactory, this.invocationId, t, null);
            } catch (IOException e) {
                logger.error("Could not write out message to channel due to", e);
                // close the channel
                IoUtils.safeClose(this.channel);
            }
            return;
        }

    }

    @Override
    protected void manageTransaction() throws Throwable {
        final int prepareResult = this.prepareTransaction();
        // write out the "prepare" result
        try {
            transactionRequestHandler.writeTxPrepareResponseMessage(this.channel, this.invocationId, prepareResult);
        } catch (IOException e) {
            logger.error("Could not write out invocation success message to channel due to", e);
            // close the channel
            IoUtils.safeClose(this.channel);
        }
    }


    private int prepareTransaction() throws Throwable {
        // first associate the tx on this thread, by resuming the tx
        final Transaction transaction = this.transactionsRepository.removeTransaction(this.xidTransactionID);
        this.resumeTransaction(transaction);
        try {
            // now "prepare"
            final Xid xid = this.xidTransactionID.getXid();
            // Courtesy: com.arjuna.ats.internal.jta.transaction.arjunacore.jca.XATerminatorImple
            final SubordinateTransaction subordinateTransaction = SubordinationManager.getTransactionImporter().getImportedTransaction(xid);
            int result = subordinateTransaction.doPrepare();
            switch (result) {
                case TwoPhaseOutcome.PREPARE_READONLY:
                    // TODO: Would it be fine to not remove the xid? (Need to understand how the subsequent
                    // flow works)
                    SubordinationManager.getTransactionImporter().removeImportedTransaction(xid);
                    return XAResource.XA_RDONLY;

                case TwoPhaseOutcome.PREPARE_OK:
                    return XAResource.XA_OK;

                case TwoPhaseOutcome.PREPARE_NOTOK:
                    // the JCA API spec limits what we can do in terms of reporting
                    // problems.
                    // try to use the exception code and cause to provide info
                    // whilst
                    // remaining API compliant. JBTM-427.
                    Exception initCause = null;
                    int xaExceptionCode = XAException.XA_RBROLLBACK;
                    try {
                        subordinateTransaction.doRollback();
                    } catch (HeuristicCommitException e) {
                        initCause = e;
                        xaExceptionCode = XAException.XAER_RMERR;
                    } catch (HeuristicMixedException e) {
                        initCause = e;
                        xaExceptionCode = XAException.XAER_RMERR;
                    } catch (SystemException e) {
                        initCause = e;
                        xaExceptionCode = XAException.XAER_RMERR;
                    } catch (final HeuristicRollbackException e) {
                        initCause = e;
                        xaExceptionCode = XAException.XAER_RMERR;
                    }
                    // remove the transaction
                    SubordinationManager.getTransactionImporter().removeImportedTransaction(xid);
                    final XAException xaException = new XAException(xaExceptionCode);
                    if (initCause != null) {
                        xaException.initCause(initCause);
                    }
                    throw xaException;

                case TwoPhaseOutcome.INVALID_TRANSACTION:
                    throw new XAException(XAException.XAER_NOTA);

                default:
                    throw new XAException(XAException.XA_RBOTHER);
            }
        } finally {
            // disassociate the tx that was associated (resumed) on this thread.
            // This needs to be done explicitly because the SubOrdinationManager isn't responsible
            // for clearing the tx context from the thread
            this.transactionsRepository.getTransactionManager().suspend();
        }
    }
}
