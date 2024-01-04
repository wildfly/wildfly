/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.mdb.timeout;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;
import org.jboss.logging.Logger;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = TransactionTimeoutQueueSetupTask.NO_TIMEOUT_JNDI_NAME)
})
public class NoTimeoutMDB implements MessageListener {
    private static final Logger log = Logger.getLogger(NoTimeoutMDB.class);
    public static final String REPLY_PREFIX = "replying ";

    @Resource(lookup = "java:/JmsXA")
    private ConnectionFactory factory;

    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @Inject
    private TransactionCheckerSingleton checker;

    @Resource
    private TransactionSynchronizationRegistry synchroRegistry;

    @Override
    public void onMessage(Message message) {
        try {
            log.tracef("onMessage received message: %s '%s'", message, ((TextMessage) message).getText());

            final Destination replyTo = message.getJMSReplyTo();

            if (replyTo == null) {
                throw new RuntimeException("ReplyTo info in message was not specified"
                    + " and bean does not know where to reply to");
            }

            TxTestUtil.enlistTestXAResource(tm.getTransaction(), checker);
            TxTestUtil.addSynchronization(tm.getTransaction(), checker);

            try (
                JMSContext context = factory.createContext()
            ) {
                context.createProducer()
                    .setJMSCorrelationID(message.getJMSMessageID())
                    .send(replyTo, REPLY_PREFIX + ((TextMessage) message).getText());
            }
        } catch (Exception e) {
            throw new RuntimeException("onMessage method execution failed", e);
        }
    }
}
