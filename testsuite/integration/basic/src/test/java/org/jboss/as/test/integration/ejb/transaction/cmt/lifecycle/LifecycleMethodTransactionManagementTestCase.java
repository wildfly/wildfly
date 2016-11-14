/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.cmt.lifecycle;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

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
