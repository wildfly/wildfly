/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.messagedrivencontext;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJBException;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.MessageDrivenBean;
import jakarta.ejb.MessageDrivenContext;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import org.jboss.logging.Logger;

/**
 * @author Jaikiran Pai
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/queue/set-message-context")
})
public class SimpleMDB implements MessageDrivenBean, MessageListener {

    private static final Logger logger = Logger.getLogger(SimpleMDB.class);

    public static final String SUCCESS_REPLY = "setMessageDrivenContext() method was invoked";

    public static final String FAILURE_REPLY = "setMessageDrivenContext() method was *not* invoked";

    @Resource(lookup = "java:/JmsXA")
    private ConnectionFactory factory;

    private MessageDrivenContext messageDrivenContext;

    @Override
    public void setMessageDrivenContext(MessageDrivenContext ctx) throws EJBException {
        this.messageDrivenContext = ctx;
    }

    @Override
    public void ejbRemove() throws EJBException {
    }


    @Override
    public void onMessage(Message message) {
        logger.trace("Received message: " + message);
        try {
            final Destination replyTo = message.getJMSReplyTo();
            if (replyTo != null) {
                logger.trace("Replying to " + replyTo);
                try (
                        JMSContext context = factory.createContext()
                ) {
                    String reply = (messageDrivenContext != null) ? SUCCESS_REPLY : FAILURE_REPLY;
                    context.createProducer()
                            .setJMSCorrelationID(message.getJMSMessageID())
                            .send(replyTo, reply);
                }
            }
        } catch (JMSException jmse) {
            throw new RuntimeException(jmse);
        }
    }
}
