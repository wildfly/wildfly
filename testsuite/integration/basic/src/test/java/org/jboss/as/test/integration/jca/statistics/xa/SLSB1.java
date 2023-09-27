/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.statistics.xa;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;

/**
 * @author dsimko@redhat.com
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class SLSB1 implements SLSB {

    @PersistenceContext(unitName = "testxapu")
    private EntityManager em;



    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @Resource
    private UserTransaction tx;

    @Inject
    private TransactionCheckerSingleton checker;

    @Override
    public void commit() throws Exception {
        tx.begin();
        TxTestUtil.enlistTestXAResource(tm.getTransaction(), checker);
        em.persist(new TestEntity());
        tx.commit();
    }

    @Override
    public void rollback() throws Exception {

        tx.begin();
        TxTestUtil.enlistTestXAResource(tm.getTransaction(), checker);

        em.persist(new TestEntity());
        em.flush();
        tx.rollback();
    }

}
