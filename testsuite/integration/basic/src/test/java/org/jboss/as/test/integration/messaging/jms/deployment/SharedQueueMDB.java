/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.jms.deployment;

import static org.jboss.as.test.integration.messaging.jms.deployment.DependentMessagingDeploymentTestCase.QUEUE_LOOKUP;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

@MessageDriven(
        activationConfig = {
            @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = QUEUE_LOOKUP),
            @ActivationConfigProperty(propertyName = "singleConnection", propertyValue = "true"),
            @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "15")
        }
)
public class SharedQueueMDB implements MessageListener {

    @Inject
    private JMSContext context;

    @Override
    public void onMessage(final Message m) {
        try {
            TextMessage message = (TextMessage) m;
            Destination replyTo = m.getJMSReplyTo();

            context.createProducer()
                    .setJMSCorrelationID(message.getJMSMessageID())
                    .send(replyTo, message.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
