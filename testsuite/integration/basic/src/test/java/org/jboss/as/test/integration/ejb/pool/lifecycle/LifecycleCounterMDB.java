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
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

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

    @EJB(lookup = "java:global/pool-ejb-callbacks-singleton/LifecycleTrackerBean!org.jboss.as.test.integration.ejb.pool.lifecycle.LifecycleTracker")
    private LifecycleTracker lifeCycleTracker;

    @Override
    public void onMessage(Message message) {
        try {
            log.trace(this + " received message " + message);
            final Destination replyTo = message.getJMSReplyTo();
            // ignore messages that need no reply
            if (replyTo == null) {
                log.trace(this + " noticed that no reply-to replyTo has been set. Just returning");
                return;
            }
            try (
                    JMSContext context = factory.createContext()
            ) {
                String reply = Constants.REPLY_MESSAGE_PREFIX + ((TextMessage) message).getText();
                context.createProducer()
                        .setJMSCorrelationID(message.getJMSMessageID())
                        .send(replyTo, reply);
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    protected void preDestroy() {

        log.trace("@PreDestroy on " + this);
        lifeCycleTracker.trackPreDestroyOn(this.getClass().getName());
    }

    @PostConstruct
    protected void postConstruct() {
        lifeCycleTracker.trackPostConstructOn(this.getClass().getName());
        log.trace(this + " MDB @PostConstructed");
    }
}