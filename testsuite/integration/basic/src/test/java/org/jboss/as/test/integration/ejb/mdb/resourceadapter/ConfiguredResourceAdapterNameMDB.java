/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.resourceadapter;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.ejb3.annotation.ResourceAdapter;
import org.jboss.logging.Logger;

/**
 * User: jpai
 */
@MessageDriven(
        name="ConfiguredResourceAdapterNameMDB",
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destination", propertyValue = ConfiguredResourceAdapterNameTestCase.QUEUE_JNDI_NAME)
})
@ResourceAdapter(value = "RARNameOverridenInJBossEJB3Xml.rar")
public class ConfiguredResourceAdapterNameMDB implements MessageListener {

    private static final Logger logger = Logger.getLogger(ConfiguredResourceAdapterNameMDB.class);

    public static final String REPLY = "Successful message delivery!";

    @EJB
    private JMSMessagingUtil jmsMessagingUtil;

    @Override
    public void onMessage(Message message) {
        logger.trace("Received message " + message);
        try {
            if (message.getJMSReplyTo() != null) {
                logger.trace("Replying to " + message.getJMSReplyTo());
                // send a reply
                this.jmsMessagingUtil.sendTextMessage(REPLY, message.getJMSReplyTo(), null);
            }
        } catch (JMSException jmse) {
            throw new RuntimeException(jmse);
        }

    }
}
