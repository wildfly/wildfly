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
