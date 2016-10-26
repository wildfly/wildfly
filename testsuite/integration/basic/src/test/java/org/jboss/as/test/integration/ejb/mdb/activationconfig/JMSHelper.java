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

package org.jboss.as.test.integration.ejb.mdb.activationconfig;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.junit.Assert.assertEquals;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;

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
