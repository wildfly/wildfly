/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
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
