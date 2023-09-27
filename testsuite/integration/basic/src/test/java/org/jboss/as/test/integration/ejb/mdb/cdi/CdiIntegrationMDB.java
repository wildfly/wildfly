/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.cdi;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.logging.Logger;

/**
 * User: jpai
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = MDBCdiIntegrationTestCase.QUEUE_JNDI_NAME)
})
public class CdiIntegrationMDB implements MessageListener {

    private static final Logger logger = Logger.getLogger(CdiIntegrationMDB.class);


    public static final String REPLY = "Successful message delivery!";

    @EJB
    private JMSMessagingUtil jmsMessagingUtil;

    @Inject
    private RequestScopedCDIBean requestScopedCDIBean;

    @Override
    public void onMessage(Message message) {
        logger.trace("Received message " + message);
        try {
            if (message.getJMSReplyTo() != null) {
                logger.trace("Replying to " + message.getJMSReplyTo());
                // send a reply
                this.jmsMessagingUtil.sendTextMessage(requestScopedCDIBean.sayHello(), message.getJMSReplyTo(), null);
            }
        } catch (JMSException jmse) {
            throw new RuntimeException(jmse);
        }

    }
}
