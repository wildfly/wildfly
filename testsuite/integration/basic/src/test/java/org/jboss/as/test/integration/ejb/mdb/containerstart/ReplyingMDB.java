/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @authors tag. See the copyright.txt in the
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
package org.jboss.as.test.integration.ejb.mdb.containerstart;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.jms.DeliveryMode.NON_PERSISTENT;

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

import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.ejb3.annotation.ResourceAdapter;
import org.jboss.logging.Logger;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/exported/queue/sendMessage"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1"),
        @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "10"),
        @ActivationConfigProperty(propertyName = "useDLQ", propertyValue = "false") })
@ResourceAdapter(value = "hornetq-ra.rar")
public class ReplyingMDB implements MessageListener {
    private static final Logger log = Logger.getLogger(ReplyingMDB.class);
    
    @Resource(lookup = "java:/ConnectionFactory")
    private ConnectionFactory factory;

    private Connection connection;
    private Session session;
    private MessageProducer sender;
    
    private static final int WAIT_S = TimeoutUtil.adjust(10);

    public void onMessage(Message m) {
        try {
            TextMessage message = (TextMessage) m;
            String text = message.getText();

            TextMessage replyMessage;
            if (message instanceof TextMessage) {
                if (text.equals("await") && !message.getJMSRedelivered()) {
                    // we have received the first message
                    HelperSingletonImpl.barrier.await(WAIT_S, SECONDS);
                    HelperSingletonImpl.barrier.reset();
                    // wait for undeploy, the MDB will be interrupted when it is undeployed
                    Thread.sleep(SECONDS.toMillis(WAIT_S));
                }
                replyMessage = session.createTextMessage("Reply: " + text);
            } else {
                replyMessage = session.createTextMessage("Unknown message");
            }
            Destination destination = message.getJMSReplyTo();
            message.setJMSDeliveryMode(NON_PERSISTENT);
            sender.send(destination, replyMessage);
            log.info("onMessage method [OK], msg: " + message.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void postConstruct() {
        log.info(ReplyingMDB.class.getSimpleName() + " was created");
        try {
            connection = factory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            sender = session.createProducer(null);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void preDestroy() {
        log.info("Destroying MDB " + ReplyingMDB.class.getSimpleName());
        try {
            if (connection != null)
                connection.close();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
