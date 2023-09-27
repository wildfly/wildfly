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
import org.jboss.logging.Logger;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;

@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destination", propertyValue = TransactionTimeoutQueueSetupTask.PROPERTY_TIMEOUT_JNDI_NAME),
    @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "1")
})
public class ActivationPropertyTimeoutMDB implements MessageListener {
    private static final Logger log = Logger.getLogger(ActivationPropertyTimeoutMDB.class);
    public static final String REPLY_PREFIX = "replying ";

    @Resource(lookup = "java:/JmsXA")
    private ConnectionFactory factory;

    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @Inject
    private TransactionCheckerSingleton checker;

    @Override
    public void onMessage(Message message) {
        try {
            log.tracef("onMessage received message: %s '%s'", message, ((TextMessage) message).getText());

            // this should mean that second attempt for calling onMessage comes to play
            if(checker.getRolledback() > 0) {
                log.tracef("Discarding message '%s' as onMessage called for second time", message);
                return;
            }

            final Destination replyTo = message.getJMSReplyTo();

            if (replyTo == null) {
                throw new RuntimeException("ReplyTo info in message was not specified"
                    + " and bean does not know where to reply to");
            }

            // should timeout txn - this timeout waiting has to be greater than 1 s
            // (see transactionTimeout activation config property)
            TxTestUtil.waitForTimeout(tm);

            TxTestUtil.enlistTestXAResource(tm.getTransaction(), checker);

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
