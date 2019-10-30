/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.security.runas.mdb;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/queue/mdbtest") })
@RunAs("TestRole")
@SecurityDomain(value = "other", unauthenticatedPrincipal = "user")
public class QueueTestMDB implements MessageListener {
    @Resource(mappedName = "java:/ConnectionFactory")
    private QueueConnectionFactory qFactory;

    @EJB
    MyStateless bean;

    public void onMessage(Message message) {
        try {
            try {
                bean.setState(((TextMessage) message).getText());
                sendReply((Queue) message.getJMSReplyTo(), message.getJMSMessageID());
            } catch (Exception e) {
                sendReply((Queue) message.getJMSReplyTo(), message.getJMSMessageID(), e);
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendReply(Queue destination, String messageID) throws JMSException {
        QueueConnection conn = qFactory.createQueueConnection();
        try {
            QueueSession session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            QueueSender sender = session.createSender(destination);
            TextMessage message = session.createTextMessage("SUCCESS");
            message.setJMSCorrelationID(messageID);
            sender.send(message, DeliveryMode.NON_PERSISTENT, 4, 500);
        } finally {
            conn.close();
        }
    }

    private void sendReply(Queue destination, String messageID, Exception e) throws JMSException {
        QueueConnection conn = qFactory.createQueueConnection();
        try {
            QueueSession session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            QueueSender sender = session.createSender(destination);
            ObjectMessage message = session.createObjectMessage(e);
            message.setJMSCorrelationID(messageID);
            sender.send(message, DeliveryMode.NON_PERSISTENT, 4, 500);
        } finally {
            conn.close();
        }
    }
}
