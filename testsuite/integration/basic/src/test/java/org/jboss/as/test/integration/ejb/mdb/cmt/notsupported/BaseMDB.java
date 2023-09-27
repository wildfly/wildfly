/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.cmt.notsupported;

import static org.jboss.as.test.integration.ejb.mdb.cmt.notsupported.ContainerManagedTransactionNotSupportedTestCase.EXCEPTION_PROP_NAME;

import jakarta.annotation.Resource;
import jakarta.ejb.MessageDrivenContext;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import org.jboss.logging.Logger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public abstract class BaseMDB implements MessageListener {

    private static final Logger logger = Logger.getLogger(BaseMDB.class);

    @Resource(lookup="java:comp/DefaultJMSConnectionFactory")
    private ConnectionFactory cf;

    @Resource
    protected MessageDrivenContext messageDrivenContext;

    @Override
    public void onMessage(Message message) {
        logger.trace("Received message: " + message);

        boolean setRollbackOnlyThrowsIllegalStateException;

        try {
            // this method in a Container-managed MDB with transaction NOT_SUPPORTED must throw an exception
            messageDrivenContext.setRollbackOnly();
            setRollbackOnlyThrowsIllegalStateException = false;
        } catch (IllegalStateException e) {
            setRollbackOnlyThrowsIllegalStateException = true;
        }

        try {
            final Destination replyTo = message.getJMSReplyTo();
            if (replyTo != null) {
                logger.trace("Replying to " + replyTo);
                try (
                        JMSContext context = cf.createContext()
                ) {
                    context.createProducer()
                            .setJMSCorrelationID(message.getJMSMessageID())
                            .setProperty(EXCEPTION_PROP_NAME, setRollbackOnlyThrowsIllegalStateException)
                            .send(replyTo, "");
                }
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
