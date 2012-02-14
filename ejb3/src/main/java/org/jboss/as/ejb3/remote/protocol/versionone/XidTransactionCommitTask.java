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

import javax.transaction.HeuristicCommitException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinateTransaction;
import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinationManager;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.logging.Logger;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.remoting3.Channel;

/**
 * @author Jaikiran Pai
 */
class XidTransactionCommitTask extends XidTransactionManagementTask {

    private static final Logger logger = Logger.getLogger(XidTransactionCommitTask.class);

    private final boolean onePhaseCommit;

    XidTransactionCommitTask(final TransactionRequestHandler txRequestHandler, final EJBRemoteTransactionsRepository transactionsRepository,
                             final MarshallerFactory marshallerFactory, final XidTransactionID xidTransactionID, final Channel channel,
                             final short invocationId, final boolean onePhaseCommit) {
        super(txRequestHandler, transactionsRepository, marshallerFactory, xidTransactionID, channel, invocationId);
        this.onePhaseCommit = onePhaseCommit;
    }

    @Override
    protected void manageTransaction() throws Throwable {
        // first associate the tx on this thread, by resuming the tx
        final Transaction transaction = this.transactionsRepository.removeTransaction(this.xidTransactionID);
        this.resumeTransaction(transaction);
        // now commit
        final Xid xid = this.xidTransactionID.getXid();
        // Courtesy: com.arjuna.ats.internal.jta.transaction.arjunacore.jca.XATerminatorImple
        try {
            // get the subordinate tx
            final SubordinateTransaction subordinateTransaction = SubordinationManager.getTransactionImporter().getImportedTransaction(xid);

            if (subordinateTransaction == null) {
                throw new XAException(XAException.XAER_INVAL);
            }

            if (subordinateTransaction.activated()) {
                if (this.onePhaseCommit) {
                    subordinateTransaction.doOnePhaseCommit();
                } else {
                    subordinateTransaction.doCommit();
                }
                // remove the tx
                SubordinationManager.getTransactionImporter().removeImportedTransaction(xid);
            } else {
                throw new XAException(XAException.XA_RETRY);
            }

        } catch (RollbackException e) {
            // remove the tx
            SubordinationManager.getTransactionImporter().removeImportedTransaction(xid);
            final XAException xaException = new XAException(XAException.XA_RBROLLBACK);
            xaException.initCause(e);
            throw xaException;

        } catch (XAException ex) {
            // resource hasn't had a chance to recover yet
            if (ex.errorCode != XAException.XA_RETRY) {
                // remove tx
                SubordinationManager.getTransactionImporter().removeImportedTransaction(xid);
            }
            throw ex;

        } catch (HeuristicRollbackException ex) {
            final XAException xaException = new XAException(XAException.XA_HEURRB);
            xaException.initCause(ex);
            throw xaException;

        } catch (HeuristicMixedException ex) {
            final XAException xaException = new XAException(XAException.XA_HEURMIX);
            xaException.initCause(ex);
            throw xaException;

        } catch (final HeuristicCommitException ex) {
            final XAException xaException = new XAException(XAException.XA_HEURCOM);
            xaException.initCause(ex);
            throw xaException;

        } catch (final IllegalStateException ex) {
            // remove tx
            SubordinationManager.getTransactionImporter().removeImportedTransaction(xid);

            final XAException xaException = new XAException(XAException.XAER_NOTA);
            xaException.initCause(ex);
            throw xaException;

        } catch (SystemException ex) {
            // remove tx
            SubordinationManager.getTransactionImporter().removeImportedTransaction(xid);

            final XAException xaException = new XAException(XAException.XAER_RMERR);
            xaException.initCause(ex);
            throw xaException;


        } finally {
            // disassociate the tx that was associated (resumed) on this thread.
            // This needs to be done explicitly because the SubOrdinationManager isn't responsible
            // for clearing the tx context from the thread
            this.transactionsRepository.getTransactionManager().suspend();
        }
    }
}
