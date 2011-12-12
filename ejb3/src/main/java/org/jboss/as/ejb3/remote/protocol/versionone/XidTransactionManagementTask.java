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

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.io.IOException;

import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.logging.Logger;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.remoting3.Channel;
import org.xnio.IoUtils;


/**
 * @author Jaikiran Pai
 */
abstract class XidTransactionManagementTask implements Runnable {

    private static final Logger logger = Logger.getLogger(XidTransactionManagementTask.class);

    protected final short invocationId;
    protected final Channel channel;
    protected final EJBRemoteTransactionsRepository transactionsRepository;
    protected final XidTransactionID xidTransactionID;
    protected final MarshallerFactory marshallerFactory;
    protected final TransactionRequestHandler transactionRequestHandler;

    XidTransactionManagementTask(final TransactionRequestHandler txRequestHandler, final EJBRemoteTransactionsRepository transactionsRepository,
                                 final MarshallerFactory marshallerFactory, final XidTransactionID xidTransactionID,
                                 final Channel channel, final short invocationId) {

        this.transactionRequestHandler = txRequestHandler;
        this.channel = channel;
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
                logger.error("Error during transaction management of transaction id " + this.xidTransactionID, t);
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

        try {
            // write out invocation success message to the channel
            transactionRequestHandler.writeTxInvocationResponseMessage(this.channel, this.invocationId);
        } catch (IOException e) {
            logger.error("Could not write out invocation success message to channel due to", e);
            // close the channel
            IoUtils.safeClose(this.channel);
        }
    }

    protected abstract void manageTransaction() throws Throwable;

    protected void resumeTransaction(final Transaction transaction) throws Exception {
        final TransactionManager transactionManager = this.transactionsRepository.getTransactionManager();
        transactionManager.resume(transaction);
    }

}
