/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.messagelistener;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.jboss.logging.Logger;

/**
 * @author Jaikiran Pai
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/queue/message-listener")
})
public class ConcreteMDB extends CommonBase {

    private static final Logger logger = Logger.getLogger(ConcreteMDB.class);

    public static final String SUCCESS_REPLY = "onMessage() method was invoked";

    public static final String FAILURE_REPLY = "onMessage() method was *not* invoked";

    @Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory factory;

    @Override
    public void onMessage(Message message) {
        logger.trace("Received message: " + message);
        try {
            final Destination replyTo = message.getJMSReplyTo();
            if (replyTo != null) {
                logger.trace("Replying to " + replyTo);
                try (
                        JMSContext context = factory.createContext()
                ) {
                    context.createProducer()
                            .setJMSCorrelationID(message.getJMSMessageID())
                            .send(replyTo, SUCCESS_REPLY);
                }
            }
        } catch (JMSException jmse) {
            throw new RuntimeException(jmse);
        }
    }
}
