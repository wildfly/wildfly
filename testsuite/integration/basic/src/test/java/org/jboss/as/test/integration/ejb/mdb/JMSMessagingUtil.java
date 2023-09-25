/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb;


import org.jboss.as.test.shared.TimeoutUtil;

import java.io.Serializable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

/**
 * User: jpai
 */
@Stateless
public class JMSMessagingUtil {

    private ConnectionFactory connectionFactory;

    private Connection connection;

    private Session session;

    @PreDestroy
    protected void preDestroy() throws JMSException {
        session.close();
        connection.close();
    }

    @PostConstruct
    protected void postConstruct() throws JMSException {
        connection = this.connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Resource(mappedName = "java:/ConnectionFactory")
    public void setConnectionFactory(final ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void sendTextMessage(final String msg, final Destination destination, final Destination replyDestination) throws JMSException {
        final TextMessage message = session.createTextMessage(msg);
        this.sendMessage(message, destination, replyDestination);
    }

    public void sendObjectMessage(final Serializable msg, final Destination destination, final Destination replyDestination) throws JMSException {
        final ObjectMessage message = session.createObjectMessage();
        message.setObject(msg);
        this.sendMessage(message, destination, replyDestination);
    }

    public void reply(final Message message) throws JMSException {
        final Destination destination = message.getJMSReplyTo();
        // ignore messages that need no reply
        if (destination == null) {
            return;
        }
        String text = (message instanceof TextMessage) ? ((TextMessage)message).getText() : message.toString();
        final Message replyMsg = session.createTextMessage(text);
        replyMsg.setJMSCorrelationID(message.getJMSMessageID());
        this.sendMessage(replyMsg, destination, null);
    }

    public Message receiveMessage(final Destination destination, final int waitInMillis) throws JMSException {
        MessageConsumer consumer = this.session.createConsumer(destination);
        try {
            return consumer.receive(TimeoutUtil.adjust(waitInMillis));
        } finally {
            consumer.close();
        }
    }

    private void sendMessage(final Message message, final Destination destination, final Destination replyDestination) throws JMSException {
        if (replyDestination != null) {
            message.setJMSReplyTo(replyDestination);
        }
        final MessageProducer messageProducer = session.createProducer(destination);
        messageProducer.send(message);
        messageProducer.close();
    }
}
