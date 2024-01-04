/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.activationconfig;

import static jakarta.jms.Session.AUTO_ACKNOWLEDGE;
import static org.junit.Assert.assertEquals;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;

import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class JMSHelper {

    private static final Logger logger = Logger.getLogger(MDBWithLookupActivationConfigProperties.class);

    public static void assertSendAndReceiveTextMessage(ConnectionFactory cf, Destination destination, String text) throws JMSException {
        try(
                JMSContext context = cf.createContext(AUTO_ACKNOWLEDGE)
        ) {
            TemporaryQueue replyTo = context.createTemporaryQueue();
            context.createProducer()
                    .setJMSReplyTo(replyTo)
                    .send(destination, text);

            String replyText = context.createConsumer(replyTo)
                    .receiveBody(String.class, TimeoutUtil.adjust(5000));
            assertEquals(text, replyText);
        }
    }

    public static void reply(ConnectionFactory cf, Message message) {
        logger.trace("Received message: " + message);
        try (
                JMSContext context = cf.createContext(AUTO_ACKNOWLEDGE)
        ) {
            if (message.getJMSReplyTo() != null) {
                logger.trace("Replying to " + message.getJMSReplyTo());

                String text = (message instanceof TextMessage) ? ((TextMessage)message).getText() : message.toString();

                context.createProducer()
                        .setJMSCorrelationID(message.getJMSMessageID())
                        .send(message.getJMSReplyTo(), text);
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
