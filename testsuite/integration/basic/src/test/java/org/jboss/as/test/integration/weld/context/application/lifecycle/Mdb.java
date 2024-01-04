/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.context.application.lifecycle;

import org.jboss.logging.Logger;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;


/**
 * @author emmartins
 */
@JMSDestinationDefinition(
        name = Mdb.JNDI_NAME,
        interfaceName = "jakarta.jms.Queue",
        destinationName = "jmsQueue"
)
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = Mdb.JNDI_NAME),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "auto-acknowledge")
})
public class Mdb implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(Mdb.class);

    public static final String JNDI_NAME = "java:app/mdb";

    public void onMessage(Message message) {
        try {
            String text = ((TextMessage) message).getText();
            LOGGER.info("Received " + text);
        } catch (Throwable e) {
            LOGGER.error("Failed to receive or send message", e);
        }
    }
}
