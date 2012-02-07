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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.*;

import org.jboss.ejb3.annotation.ResourceAdapter;
import org.jboss.logging.Logger;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.jms.DeliveryMode.NON_PERSISTENT;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/queue/sendMessage"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1"),
        @ActivationConfigProperty(propertyName = "useDLQ", propertyValue = "false") })
@ResourceAdapter(value = "hornetq-ra.rar")
public class ReplyingMDB implements MessageListener {
    private static final Logger log = Logger.getLogger(ReplyingMDB.class);
    
    @Resource(mappedName = "java:/ConnectionFactory")
    private QueueConnectionFactory factory;

    private QueueConnection connection;
    private QueueSession session;
    private QueueSender sender;

    public void onMessage(Message message) {
        try {
            TextMessage reply;
            if (message instanceof TextMessage) {
                String text = ((TextMessage) message).getText();
                if (text.equals("await")) {
                    // we have received the first message
                    HelperSingletonImpl.barrier.await(10, SECONDS);
                    HelperSingletonImpl.barrier.reset();
                    // wait for undeploy
                    HelperSingletonImpl.barrier.await(10, SECONDS);
                    HelperSingletonImpl.barrier.reset();
                }
                reply = session.createTextMessage("Reply: " + text);
            } else {
                reply = session.createTextMessage("Unknown message");
            }
            Destination destination = message.getJMSReplyTo();
            sender.send(destination, reply, NON_PERSISTENT, 1, SECONDS.toMillis(10));
            log.info("onMessage method [OK], msg: " + message.toString());
        } catch (JMSException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (BrokenBarrierException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void postConstruct() {
        log.info(ReplyingMDB.class.getSimpleName() + " was created");
        try {
            connection = factory.createQueueConnection();
            session = connection.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            sender = session.createSender(null);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void preDestroy() {
        try {
            if (sender != null)
                sender.close();
            if (session != null)
                session.close();
            if (connection != null)
                connection.close();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
