/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.jms.external;

import static org.jboss.as.test.integration.messaging.jms.external.ExternalMessagingDeploymentTestCase.QUEUE_LOOKUP;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSPasswordCredential;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

/**
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
@MessageDriven(
        activationConfig = {
            @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
            @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = QUEUE_LOOKUP),
            @ActivationConfigProperty(propertyName="user", propertyValue="guest"),
            @ActivationConfigProperty(propertyName="password", propertyValue="guest")
        }
)
public class DefaultResourceAdapterQueueMDB implements MessageListener {

    @Inject
    @JMSPasswordCredential(userName = "guest", password = "guest")
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
