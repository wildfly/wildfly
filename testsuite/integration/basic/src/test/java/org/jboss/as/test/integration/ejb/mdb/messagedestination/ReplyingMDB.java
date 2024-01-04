/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.messagedestination;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@MessageDriven(activationConfig = {
        // queue does not exist but it will be corrected by jboss-ejb3.xml
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/mdbtest/notExistingQueue")
})
public class ReplyingMDB implements MessageListener {
    @Resource(lookup = "java:/JmsXA")
    private ConnectionFactory factory;

    @Override
    public void onMessage(Message message) {
        try {
            //System.out.println("Message " + message);
            final Destination replyTo = message.getJMSReplyTo();
            // ignore messages that need no reply
            if (replyTo == null)
                return;
            try (
                    JMSContext context = factory.createContext()
            ) {
                context.createProducer()
                        .setJMSCorrelationID(message.getJMSMessageID())
                        .send(replyTo, "replying " + ((TextMessage) message).getText());
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
