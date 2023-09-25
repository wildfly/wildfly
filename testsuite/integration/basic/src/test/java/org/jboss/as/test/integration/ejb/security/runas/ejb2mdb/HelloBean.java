/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;
import javax.naming.InitialContext;

import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;

/**
 * Bean passes message to HelloMDB bean and checks the reply queue.
 *
 * @author Ondrej Chaloupka
 */
@Stateless(name = "Hello")
@Remote(Hello.class)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class HelloBean implements Hello {
    private static final Logger log = Logger.getLogger(HelloBean.class);

    public static final String SAYING = "Hello";
    public static final String QUEUE_NAME = "queue/TestQueue";
    public static final String QUEUE_NAME_JNDI = "java:jboss/exported/" + QUEUE_NAME;

    @Resource
    private SessionContext context;

    public String sayHello() throws Exception {
        InitialContext initialContext = new InitialContext();

        try {
            ConnectionFactory cf = (ConnectionFactory) initialContext.lookup("java:/ConnectionFactory");
            String replyMessage = HelloBean.sendMessage(cf);
            return String.format("%s %s, %s", SAYING, getName(), replyMessage);
        } finally {
            initialContext.close();
        }
    }

    /**
     * Helper method to send message to {@link #QUEUE_NAME}.
     */
    public static String sendMessage(ConnectionFactory cf) throws Exception {
        Connection connection = null;

        try {
            Queue queue = cf.createContext("guest", "guest").createQueue(QUEUE_NAME);
            connection = cf.createConnection("guest", "guest");
            connection.start(); // we need to start connection for consumer

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer sender = session.createProducer(queue);
            TextMessage message = session.createTextMessage("hello goodbye");
            TemporaryQueue replyQueue = session.createTemporaryQueue();
            message.setJMSReplyTo(replyQueue);
            sender.send(message);
            log.tracef("Message '%s' sent", message);

            // consume message processed by MDB
            MessageConsumer consumer = session.createConsumer(replyQueue);
            TextMessage replyMessage = (TextMessage) consumer.receive(TimeoutUtil.adjust(5000));
            log.tracef("Message '%s' received", replyMessage);

            if(replyMessage == null) {
                return "ReplyMessage is 'null'. No response received from HelloMDB."
                    + " Consult server log for details.";
            }
            return replyMessage.getText();
        } finally {
            try {
                if (connection != null) connection.close();
            } catch (JMSException jmse) {
                log.trace("connection close failed", jmse);
            }
        }

    }

    private String getName() {
        return context.getCallerPrincipal().getName();
    }
}
