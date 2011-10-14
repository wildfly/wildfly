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

import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.ejb.client.TransactionID;
import org.jboss.ejb.client.remoting.PackedInteger;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.xnio.IoUtils;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * @author Jaikiran Pai
 */
class TransactionRequestHandler extends AbstractMessageHandler {

    private static final Logger logger = Logger.getLogger(TransactionRequestHandler.class);

    private final ExecutorService executorService;
    private final EJBRemoteTransactionsRepository transactionsRepository;
    private TransactionRequestType txRequestType;

    enum TransactionRequestType {
        COMMIT,
        ROLLBACK
    }

    TransactionRequestHandler(final EJBRemoteTransactionsRepository transactionsRepository, final ExecutorService executorService, final TransactionRequestType txRequestType, final String marshallingStrategy) {
        super(marshallingStrategy);
        this.executorService = executorService;
        this.transactionsRepository = transactionsRepository;
        this.txRequestType = txRequestType;
    }

    @Override
    public void processMessage(final Channel channel, final MessageInputStream messageInputStream) throws IOException {
        final DataInputStream input = new DataInputStream(messageInputStream);
        // read the invocation id
        final short invocationId = input.readShort();
        // read the transaction id length
        final int transactionIDBytesLength = PackedInteger.readPackedInteger(input);
        // read the transaction id bytes
        final byte[] transactionIDBytes = new byte[transactionIDBytesLength];
        input.read(transactionIDBytes);
        final TransactionID transactionID = TransactionID.createTransactionID(transactionIDBytes);
        final TransactionManagementTask transactionManagementTask;
        switch (this.txRequestType) {
            case COMMIT:
                transactionManagementTask = new TransactionCommitTask(this.transactionsRepository, transactionID, channel, invocationId);
                break;
            case ROLLBACK:
                transactionManagementTask = new TransactionRollbackTask(this.transactionsRepository, transactionID, channel, invocationId);
                break;
            default:
                throw new IllegalArgumentException("Unknown transaction request type " + this.txRequestType);
        }
        // submit to a seperate thread for processing the request
        this.executorService.submit(transactionManagementTask);
    }

    private abstract class TransactionManagementTask implements Runnable {

        protected final short invocationId;
        protected final Channel channel;
        protected final EJBRemoteTransactionsRepository transactionsRepository;
        protected final TransactionID transactionID;

        TransactionManagementTask(final EJBRemoteTransactionsRepository transactionsRepository, final TransactionID transactionID,
                                  final Channel channel, final short invocationId) {
            this.channel = channel;
            this.invocationId = invocationId;
            this.transactionsRepository = transactionsRepository;
            this.transactionID = transactionID;
        }

        @Override
        public final void run() {
            try {
                this.manageTransaction(this.transactionID);
            } catch (Throwable t) {
                try {
                    // write out a failure message to the channel to let the client know that
                    // the transaction operation failed
                    TransactionRequestHandler.this.writeException(this.channel, this.invocationId, t, null);
                } catch (IOException e) {
                    logger.error("Could not write out message to channel due to", e);
                    // close the channel
                    IoUtils.safeClose(this.channel);
                }
                return;
            }

            try {
                // write out invocation success message to the channel
                TransactionRequestHandler.this.writeGenericSuccessMessage(this.channel, this.invocationId);
            } catch (IOException e) {
                logger.error("Could not write out invocation success message to channel due to", e);
                // close the channel
                IoUtils.safeClose(this.channel);
            }
        }

        protected abstract void manageTransaction(final TransactionID transactionID) throws Throwable;

        protected void resumeTransaction(final Transaction transaction) throws Exception {
            final TransactionManager transactionManager = this.transactionsRepository.getTransactionManager();
            transactionManager.resume(transaction);
        }
    }

    private class TransactionCommitTask extends TransactionManagementTask {

        TransactionCommitTask(final EJBRemoteTransactionsRepository transactionsRepository, final TransactionID transactionID, final Channel channel, final short invocationId) {
            super(transactionsRepository, transactionID, channel, invocationId);
        }

        @Override
        protected void manageTransaction(final TransactionID transactionID) throws Throwable {
            final Transaction transaction = this.transactionsRepository.removeTransaction(transactionID);
            this.resumeTransaction(transaction);
            logger.info("tx id is " + transactionID + " Tx is " + transaction + " current tx is " + this.transactionsRepository.getTransactionManager().getTransaction());
            this.transactionsRepository.getTransactionManager().commit();
        }
    }

    private class TransactionRollbackTask extends TransactionManagementTask {

        TransactionRollbackTask(final EJBRemoteTransactionsRepository transactionsRepository, final TransactionID transactionID, final Channel channel, final short invocationId) {
            super(transactionsRepository, transactionID, channel, invocationId);
        }

        @Override
        protected void manageTransaction(TransactionID transactionID) throws Throwable {
            final Transaction transaction = this.transactionsRepository.removeTransaction(transactionID);
            this.resumeTransaction(transaction);
            this.transactionsRepository.getTransactionManager().rollback();
        }
    }
}
