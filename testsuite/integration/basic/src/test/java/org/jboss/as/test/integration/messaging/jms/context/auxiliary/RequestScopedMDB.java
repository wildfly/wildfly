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

package org.jboss.as.test.integration.messaging.jms.context.auxiliary;

import static javax.ejb.TransactionManagementType.BEAN;
import static org.jboss.as.test.integration.messaging.jms.context.ScopedInjectedJMSContextTestCase.QUEUE_NAME_FOR_REQUEST_SCOPE;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionManagement;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@MessageDriven(
        name = "RequestScopedMDB",
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = QUEUE_NAME_FOR_REQUEST_SCOPE)
        }
)
@TransactionManagement(BEAN)
public class RequestScopedMDB implements MessageListener {

    @Inject
    private JMSContext context;

    public static JMSConsumer consumer;

    public void onMessage(final Message m) {
        Destination tempQueue = context.createTemporaryQueue();
        consumer = context.createConsumer(tempQueue);

        TextMessage textMessage = (TextMessage) m;
        try {
            context.createProducer()
                    .setDeliveryDelay(500)
                    .send(m.getJMSReplyTo(), textMessage.getText());
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
