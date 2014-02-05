/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote.protocol.versiontwo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

import javax.transaction.xa.Xid;

import com.arjuna.ats.jta.utils.XAHelper;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.as.ejb3.remote.protocol.AbstractMessageHandler;
import org.jboss.as.ejb3.remote.protocol.versionone.ChannelAssociation;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.ejb.client.remoting.PackedInteger;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.remoting3.MessageOutputStream;
import org.xnio.IoUtils;

/**
 * Responsible for handling a transaction "recover" message from an EJB remote client
 *
 * @author Jaikiran Pai
 */
class TransactionRecoverMessageHandler extends AbstractMessageHandler {

    private static final Logger logger = Logger.getLogger(TransactionRecoverMessageHandler.class);

    private static final byte HEADER_TX_RECOVER_RESPONSE = 0x1A;

    private final ExecutorService executorService;
    private final MarshallerFactory marshallerFactory;
    private final EJBRemoteTransactionsRepository transactionsRepository;

    TransactionRecoverMessageHandler(final EJBRemoteTransactionsRepository transactionsRepository, final MarshallerFactory marshallerFactory,
                                     final ExecutorService executorService) {
        this.executorService = executorService;
        this.transactionsRepository = transactionsRepository;
        this.marshallerFactory = marshallerFactory;
    }

    @Override
    public void processMessage(ChannelAssociation channelAssociation, InputStream inputStream) throws IOException {
        final DataInputStream input = new DataInputStream(inputStream);
        // read the invocation id
        final short invocationId = input.readShort();
        final String txParentNodeName = input.readUTF();
        final int recoveryFlags = input.readInt();
        executorService.submit(new TxRecoveryTask(channelAssociation, invocationId, txParentNodeName, recoveryFlags));
    }

    private final class TxRecoveryTask implements Runnable {

        private final String txParentNodeName;
        private final short invocationId;
        private final ChannelAssociation channelAssociation;
        private final int recoveryFlags;

        TxRecoveryTask(final ChannelAssociation channelAssociation, final short invocationId, final String txParentNodeName, final int recoveryFlags) {
            this.txParentNodeName = txParentNodeName;
            this.invocationId = invocationId;
            this.channelAssociation = channelAssociation;
            this.recoveryFlags = recoveryFlags;
        }

        @Override
        public void run() {
            Xid[] xidsToRecover = new Xid[0];
            try {
                xidsToRecover = transactionsRepository.getXidsToRecoverForParentNode(this.txParentNodeName, this.recoveryFlags);
            } catch (Throwable t) {
                try {
                    EjbLogger.ROOT_LOGGER.errorDuringTransactionRecovery(t);
                    // write out a failure message to the channel to let the client know that
                    // the transaction operation failed
                    TransactionRecoverMessageHandler.this.writeException(channelAssociation, marshallerFactory, invocationId, t, null);
                } catch (IOException e) {
                    EjbLogger.ROOT_LOGGER.couldNotWriteOutToChannel(e);
                    // close the channel
                    IoUtils.safeClose(channelAssociation.getChannel());
                }
                return;
            }
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace("Returning " + xidsToRecover.length + " Xid(s) to recover for parent node: " + txParentNodeName);
                    for (final Xid xid : xidsToRecover) {
                        logger.trace("EJB Xid to recover " + XAHelper.xidToString(xid));
                    }
                }
                // write out invocation success message to the channel
                this.writeResponse(xidsToRecover);
            } catch (IOException e) {
                EjbLogger.ROOT_LOGGER.couldNotWriteInvocationSuccessMessage(e);
                // close the channel
                IoUtils.safeClose(this.channelAssociation.getChannel());
            }
        }

        private void writeResponse(final Xid[] xids) throws IOException {
            final DataOutputStream dataOutputStream;
            final MessageOutputStream messageOutputStream;
            try {
                messageOutputStream = channelAssociation.acquireChannelMessageOutputStream();
            } catch (Exception e) {
                throw EjbLogger.ROOT_LOGGER.failedToOpenMessageOutputStream(e);
            }
            dataOutputStream = new DataOutputStream(messageOutputStream);
            try {
                // write header
                dataOutputStream.writeByte(HEADER_TX_RECOVER_RESPONSE);
                // write invocation id
                dataOutputStream.writeShort(invocationId);
                PackedInteger.writePackedInteger(dataOutputStream, xids.length);
                if (xids.length > 0) {
                    final Marshaller marshaller = TransactionRecoverMessageHandler.this.prepareForMarshalling(marshallerFactory, dataOutputStream);
                    for (int i = 0; i < xids.length; i++) {
                        marshaller.writeObject(new XidTransactionID(xids[i]));
                    }
                    // finish marshalling
                    marshaller.finish();
                }
            } finally {
                channelAssociation.releaseChannelMessageOutputStream(messageOutputStream);
                dataOutputStream.close();
            }
        }
    }

}
