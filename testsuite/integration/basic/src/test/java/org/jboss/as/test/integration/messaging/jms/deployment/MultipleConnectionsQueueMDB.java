/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.deployment;

import static org.jboss.as.test.integration.messaging.mgmt.ServerManagementOperationsTestCase.QUEUE_LOOKUP;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

/**
 * MDB configured with maxSession=15 to create 15 connections for testing.
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a>
 */
@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = QUEUE_LOOKUP),
                @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "15")
        }
)
public class MultipleConnectionsQueueMDB implements MessageListener {

    @Inject
    private JMSContext context;

    @Override
    public void onMessage(final Message m) {
        try {
            TextMessage message = (TextMessage) m;
            Destination replyTo = m.getJMSReplyTo();

            if (replyTo != null) {
                context.createProducer()
                        .setJMSCorrelationID(message.getJMSMessageID())
                        .send(replyTo, message.getText());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
