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

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.transaction.TransactionManager;

import org.jboss.as.test.integration.ejb.transaction.utils.SingletonChecker;
import org.jboss.as.test.integration.ejb.transaction.utils.TxTestUtil;
import org.jboss.logging.Logger;

@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destination", propertyValue = TransactionTimeoutQueueSetupTask.PROPERTY_TIMEOUT_JNDI_NAME),
    @ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "1"),
})
public class ActivationPropertyTimeoutMDB implements MessageListener {
    private static final Logger log = Logger.getLogger(ActivationPropertyTimeoutMDB.class);
    public static final String REPLY_PREFIX = "replying ";

    @Resource(lookup = "java:/JmsXA")
    private ConnectionFactory factory;

    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @Inject
    private SingletonChecker checker;

    @Override
    public void onMessage(Message message) {
        try {
            log.infof("onMessage received message: %s '%s'", message, ((TextMessage) message).getText());

            // this should mean that second attempt for calling onMessage comes to play
            if(checker.getRolledback() > 0) {
                log.infof("Discarding message '%s' as onMessage called for second time", message);
                return;
            }

            final Destination replyTo = message.getJMSReplyTo();

            if (replyTo == null) {
                throw new RuntimeException("ReplyTo info in message was not specified"
                    + " and bean does not know where to reply to");
            }

            TxTestUtil.enlistTestXAResource(tm.getTransaction());

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
