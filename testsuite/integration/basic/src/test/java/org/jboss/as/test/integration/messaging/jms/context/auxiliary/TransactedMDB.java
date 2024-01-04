/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.context.auxiliary;

import static jakarta.ejb.TransactionAttributeType.REQUIRED;
import static jakarta.ejb.TransactionManagementType.CONTAINER;
import static org.jboss.as.test.integration.messaging.jms.context.InjectedJMSContextTestCase.QUEUE_NAME;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.MessageDrivenContext;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionManagement;
import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@JMSDestinationDefinition(
        name = QUEUE_NAME,
        interfaceName = "jakarta.jms.Queue",
        destinationName = "InjectedJMSContextTestCaseQueue"
)
@MessageDriven(
        name = "TransactedMDB",
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
                @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = QUEUE_NAME)
        }
)
@TransactionManagement(value = CONTAINER)
@TransactionAttribute(value = REQUIRED)
public class TransactedMDB implements MessageListener {

    @Inject
    private JMSContext context;

    @Resource
    private MessageDrivenContext mdbContext;

    public void onMessage(final Message m) {
        try {
            // ignore redelivered message
            if (m.getJMSRedelivered()) {
                return;
            }

            TextMessage message = (TextMessage) m;
            Destination replyTo = m.getJMSReplyTo();

            context.createProducer()
                    .setJMSCorrelationID(message.getJMSMessageID())
                    .send(replyTo, message.getText());
            if (m.getBooleanProperty("rollback")) {
                mdbContext.setRollbackOnly();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
