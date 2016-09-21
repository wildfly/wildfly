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

import java.io.IOException;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.tm.ExtendedJBossXATerminator;
import org.jboss.tm.ImportedTransaction;
import org.xnio.IoUtils;


/**
 * @author Jaikiran Pai
 */
abstract class XidTransactionManagementTask implements Runnable {

    protected final short invocationId;
    protected final ChannelAssociation channelAssociation;
    protected final EJBRemoteTransactionsRepository transactionsRepository;
    protected final XidTransactionID xidTransactionID;
    protected final MarshallerFactory marshallerFactory;
    protected final TransactionRequestHandler transactionRequestHandler;

    XidTransactionManagementTask(final TransactionRequestHandler txRequestHandler, final EJBRemoteTransactionsRepository transactionsRepository,
                                 final MarshallerFactory marshallerFactory, final XidTransactionID xidTransactionID,
                                 final ChannelAssociation channelAssociation, final short invocationId) {

        this.transactionRequestHandler = txRequestHandler;
        this.channelAssociation = channelAssociation;
        this.marshallerFactory = marshallerFactory;
        this.invocationId = invocationId;
        this.transactionsRepository = transactionsRepository;
        this.xidTransactionID = xidTransactionID;
    }

    @Override
    public void run() {
        try {
            this.manageTransaction();
        } catch (Throwable t) {
            try {
                EjbLogger.REMOTE_LOGGER.errorDuringTransactionManagement(t, this.xidTransactionID);
                // write out a failure message to the channel to let the client know that
                // the transaction operation failed
                transactionRequestHandler.writeException(this.channelAssociation, this.marshallerFactory, this.invocationId, t, null);
            } catch (IOException e) {
                EjbLogger.REMOTE_LOGGER.couldNotWriteOutToChannel(e);
                // close the channel
                IoUtils.safeClose(this.channelAssociation.getChannel());
            }
            return;
        }

        try {
            // write out invocation success message to the channel
            transactionRequestHandler.writeTxInvocationResponseMessage(this.channelAssociation, this.invocationId);
        } catch (IOException e) {
            EjbLogger.REMOTE_LOGGER.couldNotWriteInvocationSuccessMessage(e);
            // close the channel
            IoUtils.safeClose(this.channelAssociation.getChannel());
        }
    }

    protected abstract void manageTransaction() throws Throwable;

    protected ExtendedJBossXATerminator getXAT() {
        return transactionsRepository.getXAT();
    }

    protected void resumeTransaction(final Transaction transaction) throws Exception
    {
        final TransactionManager transactionManager = this.transactionsRepository.getTransactionManager();
        transactionManager.resume(transaction);
    }

    protected ImportedTransaction tryRecoveryForImportedTransaction() throws Exception
    {
        if (getXAT().isRecoveryByNodeOrXidSupported()) {
            EjbLogger.REMOTE_LOGGER.debugf("Trying to recover an imported transaction for Xid %s", this.xidTransactionID.getXid());
            // We intentionally pass null for Xid since passing the specific Xid doesn't seem to work for some reason.
            // As for null for parentNodeName, we do that intentionally since we aren't aware of the parent node on which
            // the transaction originated
            getXAT().doRecover(null, null);
        }
        // now that recovery has been completed via the XATerminator, it's possible that the subordinate tx will have been loaded
        return getXAT().getImportedTransaction(this.xidTransactionID.getXid());
    }
}
