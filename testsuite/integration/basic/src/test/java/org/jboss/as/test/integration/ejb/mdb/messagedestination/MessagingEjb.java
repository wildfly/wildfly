/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.messagedestination;


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
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

/**
 * User: jpai
 */
@Stateless
public class MessagingEjb {

    private ConnectionFactory connectionFactory;

    private Connection connection;

    private Session session;

    @Resource(name = "myQueue")
    private Queue queue;

    @Resource(name = "myReplyQueue")
    private Queue replyQueue;

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

    public void sendTextMessage(final String msg) throws JMSException {
        final TextMessage message = session.createTextMessage(msg);
        this.sendMessage(message, queue, replyQueue);
    }


    public void reply(final Message message) throws JMSException {
        final Destination destination = message.getJMSReplyTo();
        // ignore messages that need no reply
        if (destination == null) {
            return;
        }
        final Message replyMsg = session.createTextMessage("replying to message: " + message.toString());
        replyMsg.setJMSCorrelationID(message.getJMSMessageID());
        this.sendMessage(replyMsg, destination, null);
    }

    public Message receiveMessage(final long waitInMillis) throws JMSException {
        MessageConsumer consumer = this.session.createConsumer(replyQueue);
        return consumer.receive(waitInMillis);
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
