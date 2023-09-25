/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transaction.inflow;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.ejb3.annotation.ResourceAdapter;
import org.jboss.logging.Logger;

/**
 * MDB bound to resource adapter deployed in test.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
// @MessageDriven is defined in ejb-jar.xml
@ResourceAdapter(TransactionInflowMdb.RESOURCE_ADAPTER_NAME + ".rar")
public class TransactionInflowMdb implements MessageListener {
    private static final Logger log = Logger.getLogger(TransactionInflowMdb.class);

    public static final String RESOURCE_ADAPTER_NAME = "inflow-txn-ra";

    @EJB
    private TransactionCheckerSingleton checker;

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager transactionManager;

    public void onMessage(Message msg) {
        String text = getText(msg);
        log.tracef("%s.onMessage with message: %s[%s]", this.getClass().getSimpleName(), text, msg);

        try {
            Transaction tx = transactionManager.getTransaction();
            if (tx == null || tx.getStatus() != Status.STATUS_ACTIVE) {
                log.error("Test method called without an active transaction!");
                throw new IllegalStateException("Test method called without an active transaction!");
            }
        } catch (SystemException e) {
            log.error("Cannot get the current transaction!", e);
            throw new RuntimeException("Cannot get the current transaction!", e);
        }

        enlistXAResource();
        enlistXAResource();

        checker.addMessage(text);
        log.tracef("Message '%s' processed", text);
    }

    protected void enlistXAResource() {
        log.trace(this.getClass().getSimpleName() + ".enlistXAResource()");
        try {
            TestXAResource testXAResource = new TestXAResource(checker);
            transactionManager.getTransaction().enlistResource(testXAResource);
        } catch (Exception e) {
            log.error("Could not enlist TestXAResourceUnique", e);
            throw new IllegalStateException("Could not enlist TestXAResourceUnique", e);
        }
    }

    private String getText(Message msg) {
        try {
            return ((TransactionInflowTextMessage) msg).getText();
        } catch (JMSException e) {
            throw new RuntimeException("Can't get text from message: " + msg, e);
        }
    }
}
