/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.context.auxiliary;

import static jakarta.ejb.TransactionManagementType.BEAN;
import static org.jboss.as.test.integration.messaging.jms.context.ScopedInjectedJMSContextTestCase.QUEUE_NAME_FOR_REQUEST_SCOPE;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.TransactionManagement;
import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@MessageDriven(
        name = "RequestScopedMDB",
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
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
