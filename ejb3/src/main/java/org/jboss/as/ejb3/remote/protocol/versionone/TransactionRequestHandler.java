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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.ejb.client.TransactionID;
import org.jboss.ejb.client.UserTransactionID;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.ejb.client.remoting.PackedInteger;
import org.jboss.logging.Logger;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;

/**
 * Handles a transaction message which complies with the EJB remote protocol specification
 *
 * @author Jaikiran Pai
 */
class TransactionRequestHandler extends AbstractMessageHandler {

    private static final Logger logger = Logger.getLogger(TransactionRequestHandler.class);

    private static final byte HEADER_TX_INVOCATION_RESPONSE = 0x14;

    private final ExecutorService executorService;
    private final EJBRemoteTransactionsRepository transactionsRepository;
    private final TransactionRequestType txRequestType;
    private final MarshallerFactory marshallerFactory;


    enum TransactionRequestType {
        COMMIT,
        ROLLBACK,
        PREPARE,
        FORGET,
        BEFORE_COMPLETION
    }

    TransactionRequestHandler(final EJBRemoteTransactionsRepository transactionsRepository, final MarshallerFactory marshallerFactory,
                              final ExecutorService executorService, final TransactionRequestType txRequestType) {
        this.executorService = executorService;
        this.transactionsRepository = transactionsRepository;
        this.txRequestType = txRequestType;
        this.marshallerFactory = marshallerFactory;
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
        // if it's a commit request, the read the additional "bit" which indicates whether it's a one-phase
        // commit request
        boolean onePhaseCommit = false;
        if (this.txRequestType == TransactionRequestType.COMMIT) {
            onePhaseCommit = input.readBoolean();
        }

        // start processing
        if (transactionID instanceof UserTransactionID) {
            // handle UserTransaction
            final UserTransactionManagementTask userTransactionManagementTask;
            switch (this.txRequestType) {
                case COMMIT:
                    userTransactionManagementTask = new UserTransactionCommitTask(this, this.transactionsRepository, this.marshallerFactory, (UserTransactionID) transactionID, channel, invocationId);
                    break;
                case ROLLBACK:
                    userTransactionManagementTask = new UserTransactionRollbackTask(this, this.transactionsRepository, this.marshallerFactory, (UserTransactionID) transactionID, channel, invocationId);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown transaction request type " + this.txRequestType);
            }
            // submit to a seperate thread for processing the request
            this.executorService.submit(userTransactionManagementTask);

        } else if (transactionID instanceof XidTransactionID) {
            // handle XidTransactionID
            final XidTransactionID xidTransactionID = (XidTransactionID) transactionID;
            final XidTransactionManagementTask xidTransactionManagementTask;
            switch (this.txRequestType) {
                case COMMIT:
                    xidTransactionManagementTask = new XidTransactionCommitTask(this, this.transactionsRepository, this.marshallerFactory, xidTransactionID, channel, invocationId, onePhaseCommit);
                    break;
                case PREPARE:
                    xidTransactionManagementTask = new XidTransactionPrepareTask(this, this.transactionsRepository, this.marshallerFactory, xidTransactionID, channel, invocationId);
                    break;
                case ROLLBACK:
                    xidTransactionManagementTask = new XidTransactionRollbackTask(this, this.transactionsRepository, this.marshallerFactory, xidTransactionID, channel, invocationId);
                    break;
                case FORGET:
                    xidTransactionManagementTask = new XidTransactionForgetTask(this, this.transactionsRepository, this.marshallerFactory, xidTransactionID, channel, invocationId);
                    break;
                case BEFORE_COMPLETION:
                    xidTransactionManagementTask = new XidTransactionBeforeCompletionTask(this, this.transactionsRepository, this.marshallerFactory, xidTransactionID, channel, invocationId);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown transaction request type " + this.txRequestType);
            }
            // submit to a separate thread for processing the request
            this.executorService.submit(xidTransactionManagementTask);
        }
    }


    protected void writeTxPrepareResponseMessage(final Channel channel, final short invocationId, final int xaResourceStatusCode) throws IOException {
        final DataOutputStream dataOutputStream = new DataOutputStream(channel.writeMessage());
        try {
            // write header
            dataOutputStream.writeByte(HEADER_TX_INVOCATION_RESPONSE);
            // write invocation id
            dataOutputStream.writeShort(invocationId);
            // write a "bit" to indicate that this message contains the XAResource status for a "prepare"
            // invocation
            dataOutputStream.writeBoolean(true);
            // write the XAResource status
            PackedInteger.writePackedInteger(dataOutputStream, xaResourceStatusCode);
        } finally {
            dataOutputStream.close();
        }
    }

    protected void writeTxInvocationResponseMessage(final Channel channel, final short invocationId) throws IOException {
        final DataOutputStream dataOutputStream = new DataOutputStream(channel.writeMessage());
        try {
            // write header
            dataOutputStream.writeByte(HEADER_TX_INVOCATION_RESPONSE);
            // write invocation id
            dataOutputStream.writeShort(invocationId);
            // write a "bit" to indicate that this message doesn't contain any XAResource status (i.e. not a
            // "prepare" invocation
            dataOutputStream.writeBoolean(false);
        } finally {
            dataOutputStream.close();
        }
    }

}
