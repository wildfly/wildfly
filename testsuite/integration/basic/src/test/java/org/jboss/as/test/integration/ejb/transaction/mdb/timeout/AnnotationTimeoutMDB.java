/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.mdb.timeout;

import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.transaction.TransactionManager;
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
