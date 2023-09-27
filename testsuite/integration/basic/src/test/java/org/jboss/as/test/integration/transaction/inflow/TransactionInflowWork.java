/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transaction.inflow;

import jakarta.jms.MessageListener;
import jakarta.resource.spi.UnavailableException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.resource.spi.work.Work;
import org.jboss.logging.Logger;

/**
 * Jakarta Connectors work executing onMessage method with text message payload.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
public class TransactionInflowWork implements Work {
    private static final Logger log = Logger.getLogger(TransactionInflowWork.class);

    private MessageEndpointFactory messageFactory;
    private String msg;

    TransactionInflowWork(MessageEndpointFactory messageFactory, String msg) {
        this.messageFactory = messageFactory;
        this.msg = msg;
    }

    public void run() {
        MessageEndpoint messageEndpoint;
        try {
            messageEndpoint = messageFactory.createEndpoint(null);
            if (messageEndpoint instanceof MessageListener){
                log.tracef("Calling on message on endpoint '%s' with data message '%s'", messageEndpoint, msg);
                TransactionInflowTextMessage textMessage = new TransactionInflowTextMessage(msg);
                ((MessageListener) messageEndpoint).onMessage(textMessage);
            } else {
                throw new IllegalStateException("Not supported message endpoint: " + messageEndpoint);
            }
        } catch (UnavailableException ue) {
            String msg = String.format("Not capable to create message factory '%s' endpoint", messageFactory);
            throw new IllegalStateException(msg, ue);
        }
    }

    public void release() {
        // nothing to release
    }
}
