/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.transaction.inflow;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
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