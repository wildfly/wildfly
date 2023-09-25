/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.bmt;

import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class BeanManagedTransactionsTestCase {

    @Inject
    private UserTransaction userTransaction;

    @Inject
    private BMTStateful bmtStateful;

    @Inject
    private BMTSingleton bmtSingleton;


    @Inject
    private BMTStateless bmtStateless;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test-mandatory.war");
        war.addPackage(BeanManagedTransactionsTestCase.class.getPackage());
        war.add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        return war;
    }

    @Test(expected = EJBException.class)
    public void testStatelessBeanLeaksTransactions() throws SystemException, NotSupportedException {
        try {
            //start a transaction. this transaction should be suspended before the invocation
            userTransaction.begin();

            bmtStateless.leakTransaction();
        } finally {
            userTransaction.rollback();
        }
    }

    @Test(expected = EJBException.class)
    public void testSingletonBeanLeaksTransactions() {
        bmtSingleton.leakTransaction();
    }

    @Test
    public void testStatefulBeanTransaction() throws SystemException {
        bmtStateful.createTransaction();
        Assert.assertEquals(userTransaction.getStatus(), Status.STATUS_NO_TRANSACTION);
        bmtStateful.rollbackTransaction();
        Assert.assertEquals(userTransaction.getStatus(), Status.STATUS_NO_TRANSACTION);
    }

}
