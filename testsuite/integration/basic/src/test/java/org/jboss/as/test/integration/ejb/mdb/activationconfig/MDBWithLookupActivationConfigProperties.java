/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.activationconfig;

import static org.jboss.as.test.integration.ejb.mdb.activationconfig.JMSHelper.reply;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

/**
 * This message-driven bean does not implement MessageListener interface; instead
 * it is specified with {@code messageListenerInterface} annotation.
 */
@MessageDriven (messageListenerInterface=MessageListener.class,
        activationConfig = {
        @ActivationConfigProperty(propertyName = "connectionFactoryLookup", propertyValue = "java:/ConnectionFactory"),
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = MDBWithLookupActivationConfigProperties.QUEUE_JNDI_NAME),
})
public class MDBWithLookupActivationConfigProperties {
    public static final String QUEUE_JNDI_NAME = "java:jboss/jms/mdbtest/MDBWithLookupActivationConfigProperties";

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    public void onMessage(Message message) {
        reply(connectionFactory, message);
    }
}
