/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.mdb.messagedrivencontext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

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

    private Connection connection;
    private Session session;

    private MessageDrivenContext messageDrivenContext;

    @PreDestroy
    protected void preDestroy() throws JMSException {
        session.close();
        connection.close();
    }

    @PostConstruct
    protected void postConstruct() throws JMSException {
        connection = factory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Override
    public void setMessageDrivenContext(MessageDrivenContext ctx) throws EJBException {
        this.messageDrivenContext = ctx;
    }

    @Override
    public void ejbRemove() throws EJBException {
    }


    @Override
    public void onMessage(Message message) {
        logger.info("Received message: " + message);
        try {
            if (message.getJMSReplyTo() != null) {
                logger.info("Replying to " + message.getJMSReplyTo());
                final Destination destination = message.getJMSReplyTo();
                final MessageProducer replyProducer = session.createProducer(destination);
                final Message replyMsg;
                if (this.messageDrivenContext != null) {
                    replyMsg = session.createTextMessage(SUCCESS_REPLY);
                } else {
                    replyMsg = session.createTextMessage(FAILURE_REPLY);
                }
                replyMsg.setJMSCorrelationID(message.getJMSMessageID());
                replyProducer.send(replyMsg);
                replyProducer.close();
            }
        } catch (JMSException jmse) {
            throw new RuntimeException(jmse);
        }
    }
}
