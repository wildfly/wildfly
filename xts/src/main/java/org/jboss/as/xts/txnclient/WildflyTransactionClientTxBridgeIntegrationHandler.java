/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.txn.deployment.TransactionRollbackSetupAction;
import org.jboss.as.xts.XTSHandlerDeploymentProcessor;
import org.jboss.as.xts.jandex.EndpointMetaData;
import org.jboss.as.xts.logging.XtsAsLogger;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.invocation.Invocation;
import org.wildfly.transaction.client.ContextTransactionManager;

import java.util.List;

/**
 * <p>
 *   Handler integrating the handle message event with WildFly Transaction Client (WFTC) SPI.
 * </p>
 * <p>
 *   This handler manages the outbound processing where it has to be processed
 *   before the {@link org.jboss.jbossts.txbridge.inbound.OptionalJaxWSTxInboundBridgeHandler}.
 *   Check the handler enlistment at {@link XTSHandlerDeploymentProcessor#updateXTSEndpoint(String, EndpointMetaData, List, DeploymentUnit)}.
 * <p>
 * <p>
 *   <i>NOTE:</i> the order of the Jakarta XML Web Services handlers are defined as:
 *   <q>For outbound messages handler processing starts with the first handler in the chain
 *      and proceeds in the same order as the handler chain.
 *      For inbound messages the order of processing is reversed.
 *   </q>
 * </p>
 * <p>
 *   This handler works on outbound side. The inbound side is handled by WS integration class:
 *   {@link org.jboss.as.webservices.invocation.AbstractInvocationHandler#invokeInternal(Endpoint, Invocation)}.
 *   There is the place where WFTC imports the Narayana transaction for the incoming WS message.
 * </p>
 * <p>
 *   The outbound filter is useful for suspending the WFTC wrapper transaction. Otherwise only the underlaying Narayana transaction is suspended
 *   (Narayana XTS txbridge inbound filter does so).
 *   Then when the call leaves EJB there is invoked handler {@link TransactionRollbackSetupAction} verifying existence of the transactions.
 *   If the the WFTC transaction is not suspended then the setup action rolls-back it which leads to an errors in the server log.
 * </p>
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
