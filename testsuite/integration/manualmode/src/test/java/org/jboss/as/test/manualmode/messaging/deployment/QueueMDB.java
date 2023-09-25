/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.messaging.deployment;


import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/DependentMessagingDeploymentTestCase/myQueue")
        }
)
public class QueueMDB implements MessageListener {

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
