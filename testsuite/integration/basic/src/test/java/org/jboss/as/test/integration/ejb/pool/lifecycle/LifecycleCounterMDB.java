/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.pool.lifecycle;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

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
