/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.mdb.timeout;

import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.TransactionAttribute;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.transaction.TransactionManager;
import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.logging.Logger;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;

/**
 * <p>
 * It's expected that {@link TransactionTimeout} annotation does not work for MDB
 * with {@link TransactionAttribute} set as REQUIRED because transaction is already started
 * at time of calling onMessage method and as such timeout can't be changed at that time
 * (can't change timeout of already started txn).
 * <p>
 * In other words, using {@link TransactionTimeout} does not change transaction timeout
 * for this bean.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = TransactionTimeoutQueueSetupTask.ANNOTATION_TIMEOUT_JNDI_NAME)
})
@TransactionTimeout(value = 1, unit = TimeUnit.SECONDS)
public class AnnotationTimeoutMDB implements MessageListener {
    private static final Logger log = Logger.getLogger(AnnotationTimeoutMDB.class);
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

            final Destination replyTo = message.getJMSReplyTo();

            if (replyTo == null) {
                throw new RuntimeException("ReplyTo info in message was not specified"
                    + " and bean does not know where to reply to");
            }

            TxTestUtil.enlistTestXAResource(tm.getTransaction(), checker);

            try (
                JMSContext context = factory.createContext()
            ) {
                context.createProducer()
                    .setJMSCorrelationID(message.getJMSMessageID())
                    .send(replyTo, REPLY_PREFIX + ((TextMessage) message).getText());
            }
            // would timeout txn when TransactionTimeout be cared
            TxTestUtil.waitForTimeout(tm);
        } catch (Exception e) {
            throw new RuntimeException("onMessage method execution failed", e);
        }

    }
}
