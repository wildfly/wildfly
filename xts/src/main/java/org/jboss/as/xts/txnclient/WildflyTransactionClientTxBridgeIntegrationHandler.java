/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.xts.txnclient;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;

import org.jboss.as.txn.deployment.TransactionRollbackSetupAction;
import org.jboss.as.xts.XTSHandlerDeploymentProcessor;
import org.jboss.as.xts.logging.XtsAsLogger;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * <p>
 * Handler to integrate the handle message event with WildFly Transaction client (WFTC) SPI.
 * This handler has to be defined in order to be called after the {@link OptionalJaxWSTxInboundBridgeHandler}.
 * Check the handler enlistment at {@link XTSHandlerDeploymentProcessor}.
 * <p>
 * Importing transaction for incoming WS message is handled by WS integration class: AbstractInvocationHandler
 * where the interceptor context is filled with the imported transaction.
 * <p>
 * When the call leaves eJB there is invoked handler {@link TransactionRollbackSetupAction} verifying existence of the transactions.
 * At that time the transaction should be suspended. In case of WS call where XTS transaction was imported the suspend
 * of the transaction is done in XTS handler. This handler is part of the Narayana code and the transaction is thus suspended.
 * But WFTC stores its own notion about transaction existence and none suspends the WFTC
 */
public class WildflyTransactionClientTxBridgeIntegrationHandler implements Handler<MessageContext> {

    @Override
    public boolean handleMessage(MessageContext messageContext) {
        final Boolean isOutbound = (Boolean) messageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        try {
            if (isOutbound != null && isOutbound) {
                // suspending context before returning to the client
                if(ContextTransactionManager.getInstance() != null &&
                        ContextTransactionManager.getInstance().getStatus() != Status.STATUS_NO_TRANSACTION) {
                    ContextTransactionManager.getInstance().suspend();
                }
            }
        } catch (SystemException se) {
            XtsAsLogger.ROOT_LOGGER.cannotGetTransactionStatus(messageContext, se);
        }
        // continue with message handling
        return true;
    }

    @Override
    public boolean handleFault(MessageContext context) {
        // do nothing just continue with processing
        return true;
    }

    @Override
    public void close(MessageContext context) {
        // no action needed
    }

}