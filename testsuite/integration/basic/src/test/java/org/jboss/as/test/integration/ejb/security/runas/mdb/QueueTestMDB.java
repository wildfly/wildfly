/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runas.mdb;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.TextMessage;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
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
