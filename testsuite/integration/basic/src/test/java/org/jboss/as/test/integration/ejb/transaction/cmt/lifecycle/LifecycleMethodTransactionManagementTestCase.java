/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.cmt.lifecycle;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

import org.junit.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * tests the transactional behaviour of EJB lifecycle methods
 */
@RunWith(Arquillian.class)
public class LifecycleMethodTransactionManagementTestCase {

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejbLifecycleAnnotations.jar");
        jar.addPackage(LifecycleMethodTransactionManagementTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testDefaultRequired() throws NamingException {
        StatelessRequired2LifecycleBean required = (StatelessRequired2LifecycleBean)new InitialContext().lookup("java:module/StatelessRequired2LifecycleBean");
        Assert.assertEquals(Status.STATUS_ACTIVE, required.getState());
    }

    @Test
    public void testStatelessRequiredAsRequiresNew() throws SystemException, NotSupportedException, NamingException {
        UserTransaction userTransaction = (UserTransaction) new InitialContext().lookup("java:jboss/UserTransaction");
        TransactionSynchronizationRegistry tsr = (TransactionSynchronizationRegistry) new InitialContext().lookup("java:jboss/TransactionSynchronizationRegistry");
        userTransaction.begin();
        StatelessRequiredLifecycleBean required = (StatelessRequiredLifecycleBean)new InitialContext().lookup("java:module/StatelessRequiredLifecycleBean");
        try {
            Object key = tsr.getTransactionKey();
            Assert.assertNotSame(key, required.getKey());
            Assert.assertEquals(Status.STATUS_ACTIVE, required.getState());
        } finally {
            userTransaction.rollback();
        }
    }

    @Test
    public void testNeverRequired() throws SystemException, NotSupportedException, NamingException {
        UserTransaction userTransaction = (UserTransaction) new InitialContext().lookup("java:jboss/UserTransaction");
        userTransaction.begin();
        StatelessNeverLifecycleBean never = (StatelessNeverLifecycleBean)new InitialContext().lookup("java:module/StatelessNeverLifecycleBean");
        try {
            Assert.assertEquals(Status.STATUS_NO_TRANSACTION, never.getState());
        } finally {
            userTransaction.rollback();
        }
    }


    @Test
    public void testStatefulRequiresNew() throws SystemException, NotSupportedException, NamingException {
        UserTransaction userTransaction = (UserTransaction) new InitialContext().lookup("java:jboss/UserTransaction");
        TransactionSynchronizationRegistry tsr = (TransactionSynchronizationRegistry) new InitialContext().lookup("java:jboss/TransactionSynchronizationRegistry");
        userTransaction.begin();
        StatefulRequiresNewLifecycleBean required = (StatefulRequiresNewLifecycleBean)new InitialContext().lookup("java:module/StatefulRequiresNewLifecycleBean");
        try {
            Object key = tsr.getTransactionKey();
            Assert.assertNotSame(key, required.getKey());
            Assert.assertEquals(Status.STATUS_ACTIVE, required.getState());
        } finally {
            userTransaction.rollback();
        }
    }
}
