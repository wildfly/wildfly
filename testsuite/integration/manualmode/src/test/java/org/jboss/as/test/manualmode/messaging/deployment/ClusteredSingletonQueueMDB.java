/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.messaging.deployment;

import org.jboss.logging.Logger;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

// destinationLookup must match ClusteredSingletonMDBTestCase.QUEUE_JNDI_LOCAL
@MessageDriven(name = "ClusteredSingletonQueueMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:jboss/jms/queue/ClusteredSingletonMDBQueue"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")})
public class ClusteredSingletonQueueMDB implements MessageListener {

    private static final Logger log = Logger.getLogger(ClusteredSingletonQueueMDB.class);

    @Override
    public void onMessage(Message rcvMessage) {
        try {
            if (rcvMessage instanceof TextMessage msg) {
                log.info("Received Message from queue: " + msg.getText());
            } else {
                log.warn("Message of wrong type: " + rcvMessage.getClass().getName());
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
