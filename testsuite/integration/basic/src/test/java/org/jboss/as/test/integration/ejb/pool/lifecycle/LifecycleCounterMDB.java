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
import javax.naming.NamingException;

import org.jboss.logging.Logger;


/**
 * @author baranowb
 * @author Jaikiran Pai - Updates related to https://issues.jboss.org/browse/WFLY-1506
 */
@MessageDriven(activationConfig = {@ActivationConfigProperty(propertyName = "destination", propertyValue = Constants.QUEUE_JNDI_NAME)})
public class LifecycleCounterMDB implements MessageListener {

    private static final Logger log = Logger.getLogger(LifecycleCounterMDB.class.getName());

    @Resource(lookup = "java:/JmsXA")
    private ConnectionFactory factory;

    private Connection connection;
    private Session session;

    @EJB(lookup = "java:global/pool-ejb-callbacks-singleton/LifecycleTrackerBean!org.jboss.as.test.integration.ejb.pool.lifecycle.LifecycleTracker")
    private LifecycleTracker lifeCycleTracker;

    @Override
    public void onMessage(Message message) {
        try {
            log.info(this + " received message " + message);
            final Destination destination = message.getJMSReplyTo();
            // ignore messages that need no reply
            if (destination == null) {
                log.info(this + " noticed that no reply-to destination has been set. Just returning");
                return;
            }
            final MessageProducer replyProducer = session.createProducer(destination);
            final Message replyMsg = session.createTextMessage(Constants.REPLY_MESSAGE_PREFIX + ((TextMessage) message).getText());
            replyMsg.setJMSCorrelationID(message.getJMSMessageID());
            replyProducer.send(replyMsg);
            replyProducer.close();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    protected void preDestroy() throws JMSException {

        log.info("@PreDestroy on " + this);
        try {
            lifeCycleTracker.trackPreDestroyOn(this.getClass().getName());
        } finally {
            // closing the connection will close the session too (see javadoc of javax.jms.Connection#close())
            safeClose(this.connection);
        }
    }

    @PostConstruct
    protected void postConstruct() throws JMSException, NamingException {
        lifeCycleTracker.trackPostConstructOn(this.getClass().getName());
        log.info(this + " MDB @PostConstructed");

        this.connection = this.factory.createConnection();
        this.session = this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    static void safeClose(final Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (Throwable t) {
            // just log
            log.info("Ignoring a problem which occurred while closing: " + connection, t);
        }
    }
}