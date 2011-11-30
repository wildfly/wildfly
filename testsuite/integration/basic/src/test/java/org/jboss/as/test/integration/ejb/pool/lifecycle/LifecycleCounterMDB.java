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
package org.jboss.as.test.integration.ejb.pool.lifecycle;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
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
import javax.naming.InitialContext;
import javax.naming.NamingException;



/**
 * @author baranowb
 */
@MessageDriven(activationConfig = { @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/test") })
public class LifecycleCounterMDB implements MessageListener {//TODO: extend ReplyingMDB ?

    private static final Logger log = Logger.getLogger(LifecycleCounterMDB.class.getName());

    @Resource(lookup = "java:/JmsXA")
    private ConnectionFactory factory;

    private Connection connection;
    private Session session;

    // TODO: injection here does not work.
    // @EJB
    private LifecycleCounter lifeCycleCounter;

    @Override
    public void onMessage(Message message) {
        try {
            System.out.println("Message " + message);
            final Destination destination = message.getJMSReplyTo();
            // ignore messages that need no reply
            if (destination == null)
                return;
            final MessageProducer replyProducer = session.createProducer(destination);
            final Message replyMsg = session.createTextMessage("replying " + ((TextMessage) message).getText());
            replyMsg.setJMSCorrelationID(message.getJMSMessageID());
            replyProducer.send(replyMsg);
            replyProducer.close();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    protected void preDestroy() throws JMSException {

        this.log.info("MDB about to be gone, releasing resources");
        this.session.close();
        this.connection.close();
        this.lifeCycleCounter.incrementPreDestroyCount();
    }

    @PostConstruct
    protected void postConstruct() throws JMSException, NamingException {
        this.log.info("MDB created, initializing");
        this.connection = this.factory.createConnection();
        this.session = this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        this.lifeCycleCounter = (LifecycleCounter) new InitialContext()
                .lookup("java:global/pool-ejb-callbacks-singleton/LifecycleCounterBean!org.jboss.as.test.integration.ejb.pool.lifecycle.LifecycleCounter");
        this.lifeCycleCounter.incrementPostCreateCount();
    }
}