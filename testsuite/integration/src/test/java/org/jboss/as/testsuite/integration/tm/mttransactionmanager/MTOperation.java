/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.testsuite.integration.tm.mttransactionmanager;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.logging.Logger;
import org.jboss.tm.TxUtils;

/**
 * MultiThreaded Operations that can be executed concurrently.
 * <p/>
 * Based on Operation class.
 *
 * @author <a href="dimitris@jboss.org">Dimitris Andreadis</a>
 * @author istudens@redhat.com
 */
public class MTOperation implements Serializable {
    // Static Data ---------------------------------------------------

    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = 2924873545494045020L;
    /**
     * Available Operations
     */
    public static final int TM_GET_STATUS = 0;
    public static final int TM_BEGIN = 1;
    public static final int TM_RESUME = 2;
    public static final int TM_COMMIT = 3;
    public static final int TX_COMMIT = 4;
    public static final int TX_REGISTER_SYNC = 5;
    public static final int XX_SLEEP_200 = 6;
    public static final int XX_WAIT_FOR = 7;

    /**
     * The Logger
     */
    protected static Logger log;

    /**
     * TM instance
     */
    protected static TransactionManager tm = null;

    /**
     * Shared resources
     */
    protected static Map resources = Collections.synchronizedMap(new HashMap());

    /**
     * Active Transactions
     */
    protected static Map transactions = Collections.synchronizedMap(new HashMap());

    // Protected Data ------------------------------------------------

    /**
     * An id for this transaction
     */
    protected Integer id;

    /**
     * The operation to execute
     */
    protected int op;

    /**
     * Set when an exception is expected
     */
    protected Throwable throwable;

    /**
     * If a throwable is required, or only *may* occur
     */
    private boolean require;

    // Static Methods ------------------------------------------------

    /**
     * Setup static objects for the test
     */
    public static void init(Logger log) throws Exception {
        MTOperation.log = log;

        if (getTM().getTransaction() != null) {
            throw new IllegalStateException("Invalid thread association " + getTM().getTransaction());
        }
        resources.clear();
        transactions.clear();
    }

    /**
     * Lazy TransactionManager lookup
     */
    public static TransactionManager getTM() throws Exception {
        if (tm == null) {
            tm = (TransactionManager) new InitialContext().lookup("java:jboss/TransactionManager");
        }
        return tm;
    }

    /**
     * Cleanup
     */
    public static void destroy() {
        resources.clear();
        transactions.clear();
    }

    // Constructors --------------------------------------------------

    public MTOperation(int op) {
        this(op, 0);
    }

    public MTOperation(int op, int id) {
        this.id = new Integer(id);
        this.op = op;
    }

    public MTOperation(int op, int id, Throwable throwable) {
        this(op, id, throwable, true);
    }

    public MTOperation(int op, int id, Throwable throwable, boolean require) {
        this.id = new Integer(id);
        this.op = op;
        this.throwable = throwable;
        this.require = require;
    }

    // Public Methods ------------------------------------------------

    public void perform() throws Exception {
        Throwable caught = null;
        try {
            switch (op) {
                case TM_GET_STATUS:
                    tmGetStatus();
                    break;

                case TM_BEGIN:
                    tmBegin();
                    break;

                case TM_RESUME:
                    tmResume();
                    break;

                case TM_COMMIT:
                    tmCommit();
                    break;

                case TX_COMMIT:
                    txCommit();
                    break;

                case TX_REGISTER_SYNC:
                    txRegisterSync();
                    break;

                case XX_SLEEP_200:
                    xxSleep200();
                    break;

                case XX_WAIT_FOR:
                    xxWaitForTx();
                    break;

                default:
                    throw new IllegalArgumentException("Invalid operation " + op);
            }
        } catch (Throwable t) {
            caught = t;
        }

        // required an exception but caught none
        if (require && throwable != null && caught == null) {
            throw new Exception("Expected throwable " + throwable);
        }

        // got an exception, but it was the wrong one
        if (throwable != null && caught != null && !throwable.getClass().isAssignableFrom(caught.getClass())) {
            log.warn("Caught wrong throwable", caught);
            throw new Exception("Expected throwable " + throwable + " caught " + caught);
        }

        // did not expect an exception but caught one
        if (throwable == null && caught != null) {
            log.warn("Caught unexpected throwable", caught);
            throw new Exception("Unexpected throwable " + caught);
        }
    }

    public void tmGetStatus() throws Exception {
        log.info(tid() + " " + TxUtils.getStatusAsString(getTM().getStatus()));
    }

    public void tmBegin() throws Exception {
        log.info(tid() + " TM_BEGIN (" + id + ")");
        getTM().begin();
        Transaction tx = getTM().getTransaction();
        synchronized (transactions) {
            transactions.put(id, tx);
            transactions.notifyAll();
        }
    }

    public void tmResume() throws Exception {
        log.info(tid() + " TM_RESUME (" + id + ")");
        Transaction tx = (Transaction) transactions.get(id);
        if (tx == null) {
            throw new IllegalStateException("Tx not found:" + id);
        } else {
            getTM().resume(tx);
        }
    }

    public void tmCommit() throws Exception {
        log.info(tid() + " TM_COMMIT");
        getTM().commit();
    }

    public void txCommit() throws Exception {
        log.info(tid() + " TX_COMMIT (" + id + ")");
        Transaction tx = (Transaction) transactions.get(id);
        if (tx == null) {
            throw new IllegalStateException("Tx not found: " + id);
        } else {
            tx.commit();
        }
    }

    public void txRegisterSync() throws Exception {
        log.info(tid() + " TX_REGISTER_SYNC (" + id + ")");
        Transaction tx = (Transaction) transactions.get(id);
        if (tx == null) {
            throw new IllegalStateException("Tx not found: " + id);
        }
        Synchronization sync = new Synchronization() {
            public void beforeCompletion() {
                log.info(tid() + " beforeCompletion() called");
            }

            public void afterCompletion(int status) {
                log.info(tid() + " afterCompletion(" + TxUtils.getStatusAsString(status) + ") called");
            }
        };
        tx.registerSynchronization(sync);
    }

    public void xxWaitForTx() throws Exception {
        log.info(tid() + " XX_WAIT_FOR (" + id + ")");

        Transaction tx = (Transaction) transactions.get(id);
        while (tx == null) {
            log.info(tid() + " Sleeping for 100 msecs");
            synchronized (transactions) {
                try {
                    transactions.wait(100);
                } catch (InterruptedException ignore) {
                }
            }
            tx = (Transaction) transactions.get(id);
        }
        log.info(tid() + " Got it");
    }

    public void xxSleep200() throws Exception {
        log.info(tid() + " XX_SLEEP_200");
        Thread.sleep(200);
    }

    private String tid() {
        return Thread.currentThread().getName();
    }
}
