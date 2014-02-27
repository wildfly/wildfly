/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.testsuite.integration.secman.mdb;

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
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * MDB which reads a system property and replies whether it could read it or not.
 * The property name is taken from the received message.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/propertyTestQueue")
})
public class ReadSystemPropertyMDB implements MessageListener {
    @Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory factory;

    private Connection connection;
    private Session session;

    @Override
    public void onMessage(Message message) {
        try {
            final Destination destination = message.getJMSReplyTo();
            if (destination == null)
                return;

            final String propertyName = ((TextMessage) message).getText();
            String result;
            try {
                System.getProperty(propertyName, "");
                result = "got it!";
            } catch (Throwable e) {
                result = "failed";
            }

            final MessageProducer replyProducer = session.createProducer(destination);
            final Message replyMsg = session.createTextMessage(result);
            replyMsg.setJMSCorrelationID(message.getJMSMessageID());
            replyProducer.send(replyMsg);
            replyProducer.close();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

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

}
