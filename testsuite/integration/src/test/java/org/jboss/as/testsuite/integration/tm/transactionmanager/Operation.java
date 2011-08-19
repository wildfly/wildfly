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
package org.jboss.as.testsuite.integration.tm.transactionmanager;

import java.io.Serializable;
import java.util.HashMap;

import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.logging.Logger;

/**
 * Operations
 *
 * @author Adrian@jboss.org
 * @author istudens@redhat.com
 */
public class Operation implements Serializable {
    static final long serialVersionUID = 6263843332702708629L;
    public static final int BEGIN = -1;
    public static final int COMMIT = -2;
    public static final int ROLLBACK = -3;
    public static final int SUSPEND = -4;
    public static final int RESUME = -5;
    public static final int SETROLLBACK = -6;
    public static final int STATUS = -7;
    public static final int STATE = 0;
    public static final int CREATE = 1;
    public static final int ENLIST = 2;
    public static final int DIFFRM = 3;
    public static final int SETSTATUS = 4;
    public static final int CREATE_LOCAL = 5;
    public static final int FAIL_LOCAL = 6;

    static HashMap resources = new HashMap();
    static HashMap transactions = new HashMap();

    static Logger log;

    static TransactionManager tm = null;

    Integer id;
    int op;
    int status;
    Throwable throwable;

    public Operation(int op, int id) {
        this(op, id, 0);
    }

    public Operation(int op, int id, int status) {
        this(op, id, status, null);
    }

    public Operation(int op, int id, int status, Throwable throwable) {
        this.id = new Integer(id);
        this.op = op;
        this.status = status;
        this.throwable = throwable;
    }

    public void perform() throws Exception {
        Throwable caught = null;
        try {
            switch (op) {
                case BEGIN:
                    begin();
                    break;
                case COMMIT:
                    commit();
                    break;
                case ROLLBACK:
                    rollback();
                    break;
                case SUSPEND:
                    suspend();
                    break;
                case RESUME:
                    resume();
                    break;
                case SETROLLBACK:
                    setRollbackOnly();
                    break;
                case STATUS:
                    checkStatus();
                    break;
                case STATE:
                    checkState();
                    break;
                case CREATE:
                    create();
                    break;
                case CREATE_LOCAL:
                    createLocal();
                    break;
                case FAIL_LOCAL:
                    failLocal();
                    break;
                case ENLIST:
                    enlist();
                    break;
                case DIFFRM:
                    differentRM();
                    break;
                case SETSTATUS:
                    setStatus();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid operation " + op);
            }
        } catch (Throwable t) {
            caught = t;
        }
        if (throwable != null && caught == null)
            throw new Exception("Expected throwable " + throwable);
        if (throwable != null && (throwable.getClass().isAssignableFrom(caught.getClass())) == false) {
            caught.printStackTrace();
            throw new Exception("Expected throwable " + throwable + " was " + caught);
        }
        if (throwable == null && caught != null) {
            caught.printStackTrace();
            throw new Exception("Unexpected throwable " + caught);
        }
    }

    public void begin() throws Exception {
        log.info("BEGIN " + id);
        getTM().begin();
        Transaction tx = getTM().getTransaction();
        transactions.put(id, tx);
        log.info("BEGUN " + tx);
    }

    public void commit() throws Exception {
        log.info("COMMIT " + id);
        assertTx(id);
        getTM().commit();
        int status = getTM().getStatus();
        if (Status.STATUS_NO_TRANSACTION != status)
            throw new Exception("Expected no thread association after commit status=" + status);
        log.info("COMMITTED " + id);
    }

    public void rollback() throws Exception {
        log.info("ROLLBACK " + id);
        assertTx(id);
        getTM().rollback();
        int status = getTM().getStatus();
        if (Status.STATUS_NO_TRANSACTION != status)
            throw new Exception("Expected no thread association after rollback status=" + status);
        log.info("ROLLEDBACK " + id);
    }

    public void suspend() throws Exception {
        log.info("SUSPEND " + id);
        assertTx(id);
        getTM().suspend();
        log.info("SUSPENDED " + id);
    }

    public void resume() throws Exception {
        log.info("RESUME " + id);
        getTM().resume(getTx(id));
        assertTx(id);
        log.info("RESUMED " + id);
    }

    public void setRollbackOnly() throws Exception {
        log.info("SETROLLBACK " + id);
        getTx(id).setRollbackOnly();
        log.info("SETTEDROLLBACK " + id);
    }

    public void checkStatus() throws Exception {
        log.info("CHECKSTATUS " + id);
        int actualStatus = getTx(id).getStatus();
        log.info("CHECKINGSTATUS " + id + " Expected " + status + " was " + actualStatus);
        if (actualStatus != status)
            throw new Exception("Transaction " + id + " Expected status " + status + " was " + actualStatus);
    }

    public void checkState() throws Exception {
        log.info("CHECKSTATE " + id);
        int actualStatus = getRes(id).getStatus();
        log.info("CHECKINGSTATE " + id + " Expected " + status + " was " + actualStatus);
        if (actualStatus != status)
            throw new Exception("Resource " + id + " Expected state " + status + " was " + actualStatus);
    }

    public void create() throws Exception {
        log.info("CREATE " + id);
        Resource res = new Resource(id);
        resources.put(id, res);
        log.info("CREATED " + res);
    }

    public void createLocal() throws Exception {
        log.info("CREATE_LOCAL " + id);
        Resource res = new LocalResource(id);
        resources.put(id, res);
        log.info("CREATED_LOCAL " + res);
    }

    public void enlist() throws Exception {
        log.info("ENLIST " + id);
        Transaction tx = getTM().getTransaction();
        if (tx.enlistResource(getRes(id)) == false)
            throw new Exception("Unable to enlist resource");
        log.info("ENLISTED " + id + " " + tx);
    }

    public void differentRM() throws Exception {
        log.info("DIFFRM " + id);
        getRes(id).newResourceManager();
    }

    public void failLocal() throws Exception {
        log.info("FAIL_LOCAL " + id);
        LocalResource resource = (LocalResource) getRes(id);
        resource.failLocal();
    }

    public void setStatus() throws Exception {
        log.info("SETSTATUS " + id + " " + status);
        getRes(id).setStatus(status);
        log.info("SETTEDSTATUS " + id + " " + status);
    }

    public static void start(Logger log)
            throws Exception {
        Operation.log = log;
        if (getTM().getTransaction() != null)
            throw new IllegalStateException("Invalid thread association " + getTM().getTransaction());
        reset();
    }

    public static void end() {
        reset();
    }

    public static void reset() {
        resources.clear();
        transactions.clear();
    }

    public static void tidyUp() {
        try {
            if (getTM().getStatus() != Status.STATUS_NO_TRANSACTION) {
                log.warn("TIDYING UP AFTER BROKEN TEST!");
                getTM().rollback();
            }
        } catch (Exception ignored) {
        }
    }

    public Resource getRes(Integer id) {
        Resource res = (Resource) resources.get(id);
        if (res == null)
            throw new IllegalStateException("No resource: " + id);
        return res;
    }

    public Transaction getTx(Integer id) {
        Transaction tx = (Transaction) transactions.get(id);
        if (tx == null)
            throw new IllegalStateException("No transaction: " + id);
        return tx;
    }

    public void assertTx(Integer id)
            throws Exception {
        Transaction tx = getTx(id);
        Transaction current = getTM().getTransaction();
        log.info("Asserting tx " + tx + " current " + current);
        if (tx.equals(current) == false)
            throw new IllegalStateException("Expected tx " + tx + " was " + current);
    }

    public static TransactionManager getTM()
            throws Exception {
        if (tm == null)
            tm = (TransactionManager) new InitialContext().lookup("java:jboss/TransactionManager");
        return tm;
    }
}
