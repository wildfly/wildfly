/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.objectmessage;

import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.logging.Logger;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;

/**
 * User: jpai
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = MDBAcceptingObjectMessage.QUEUE_JNDI_NAME)
})
public class MDBAcceptingObjectMessage implements MessageListener {

    private static final Logger logger = Logger.getLogger(MDBAcceptingObjectMessage.class);

    public static final String QUEUE_JNDI_NAME = "java:jboss/jms/mdbtest/objectmessage-queue";

    @EJB
    private JMSMessagingUtil jmsMessagingUtil;

    @Override
    public void onMessage(Message message) {
        logger.trace("Received message: " + message);
        if (message instanceof ObjectMessage == false) {
            throw new RuntimeException(this.getClass().getName() + " only accepts ObjectMessage. " + message + " isn't an ObjectMessage");
        }
        try {
            // get the underlying message
            SimpleMessageInEarLibJar underlyingMessage = (SimpleMessageInEarLibJar) ((ObjectMessage) message).getObject();
            if (message.getJMSReplyTo() != null) {
                logger.trace("Replying to " + message.getJMSReplyTo());
                // create an ObjectMessage as a reply and send it to the reply queue
                this.jmsMessagingUtil.sendObjectMessage(underlyingMessage, message.getJMSReplyTo(), null);
            }
        } catch (JMSException jmse) {
            throw new RuntimeException(jmse);
        }
    }

}
