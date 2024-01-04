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
 * User: jpai
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = MDBWithUnknownActivationConfigProperties.QUEUE_JNDI_NAME),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "foo", propertyValue = "bar") // activation config properties which aren't supported by the resource adapter
})
public class MDBWithUnknownActivationConfigProperties implements MessageListener {

    public static final String QUEUE_JNDI_NAME = "java:jboss/jms/mdbtest/unknown-activationconfig-props";

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Override
    public void onMessage(Message message) {
        reply(connectionFactory, message);
    }
}
