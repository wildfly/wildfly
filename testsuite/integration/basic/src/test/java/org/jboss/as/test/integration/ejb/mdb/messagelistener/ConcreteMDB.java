/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.mdb.messagelistener;

import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * @author Jaikiran Pai
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/queue/message-listener")
})
public class ConcreteMDB extends CommonBase {

    private static final Logger logger = Logger.getLogger(ConcreteMDB.class);

    public static final String SUCCESS_REPLY = "onMessage() method was invoked";

    public static final String FAILURE_REPLY = "onMessage() method was *not* invoked";

    @Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory factory;

    private Connection connection;
    private Session session;

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
    public void onMessage(Message message) {
        logger.info("Received message: " + message);
        try {
            if (message.getJMSReplyTo() != null) {
                logger.info("Replying to " + message.getJMSReplyTo());
                final Destination destination = message.getJMSReplyTo();
                final MessageProducer replyProducer = session.createProducer(destination);
                final Message replyMsg = session.createTextMessage(SUCCESS_REPLY);
                replyMsg.setJMSCorrelationID(message.getJMSMessageID());
                replyProducer.send(replyMsg);
                replyProducer.close();
            }
        } catch (JMSException jmse) {
            throw new RuntimeException(jmse);
        }
    }
}
