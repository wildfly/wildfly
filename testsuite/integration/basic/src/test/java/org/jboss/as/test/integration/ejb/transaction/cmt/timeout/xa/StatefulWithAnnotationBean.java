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

package org.jboss.as.test.integration.ejb.transaction.cmt.timeout.xa;

import javax.annotation.Resource;
import javax.ejb.AfterBegin;
import javax.ejb.AfterCompletion;
import javax.ejb.BeforeCompletion;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.test.integration.ejb.transaction.utils.SingletonChecker;
import org.jboss.as.test.integration.ejb.transaction.utils.TxTestUtil;
import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.logging.Logger;

@Stateful
public class StatefulWithAnnotationBean {
    private static final Logger log = Logger.getLogger(StatefulWithAnnotationBean.class);

    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @Resource
    private SessionContext context;

    @Inject
    private SingletonChecker checker;

    @AfterBegin
    public void afterBegin() {
        log.info("afterBegin called");
        checker.setSynchronizedBegin();
    }

    @BeforeCompletion
    public void beforeCompletion() {
        log.info("beforeCompletion called");
        checker.setSynchronizedBefore();
    }

    @AfterCompletion
    public void afterCompletion(boolean isCommitted) {
        log.infof("afterCompletion: transaction was%s committed", isCommitted ? "" : " not");
        checker.setSynchronizedAfter(isCommitted);
    }

    public void testTransaction() throws SystemException, InterruptedException {
        Transaction txn;
        txn = tm.getTransaction();
        
        TxTestUtil.enlistTestXAResource(txn); 
        TxTestUtil.enlistTestXAResource(txn);
    }

    public void testTransactionRollbackOnly() throws SystemException, InterruptedException {
        Transaction txn;
        txn = tm.getTransaction();
        
        TxTestUtil.enlistTestXAResource(txn);
        context.setRollbackOnly();
        TxTestUtil.enlistTestXAResource(txn);
    }

    @TransactionTimeout(value = 1)
    public void testTransactionTimeout() throws SystemException, InterruptedException {
        Transaction txn;
        txn = tm.getTransaction();

        TxTestUtil.enlistTestXAResource(txn); 
        TxTestUtil.enlistTestXAResource(txn);

        TxTestUtil.waitForTimeout(tm);
    }

    @TransactionTimeout(value = 1)
    public void testTransactionTimeoutSynchroAdded() throws SystemException, InterruptedException {
        Transaction txn;
        txn = tm.getTransaction();

        TxTestUtil.addSynchronization(txn);
        TxTestUtil.enlistTestXAResource(txn); 
        TxTestUtil.enlistTestXAResource(txn);
        
        TxTestUtil.waitForTimeout(tm);
    }

    public void touch() {
        log.info("Bean touched");
    }
}
