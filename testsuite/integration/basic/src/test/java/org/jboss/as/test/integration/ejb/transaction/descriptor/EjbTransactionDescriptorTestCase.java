/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.descriptor;

import jakarta.ejb.EJBException;
import jakarta.ejb.EJBTransactionRequiredException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that is is possible to set a different transaction type from local and remote interfaces
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EjbTransactionDescriptorTestCase {

    @ArquillianResource
    private InitialContext initialContext;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test-tx-descriptor.jar");
        jar.addPackage(EjbTransactionDescriptorTestCase.class.getPackage());
        jar.addAsManifestResource(EjbTransactionDescriptorTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void testLocalMethodHasNever() throws SystemException, NotSupportedException, NamingException {
        final UserTransaction userTransaction = (UserTransaction)new InitialContext().lookup("java:jboss/UserTransaction");
        final TransactionLocal bean = (TransactionLocal) initialContext.lookup("java:module/" + DescriptorBean.class.getSimpleName() + "!" + TransactionLocal.class.getName());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, bean.transactionStatus());

        try {
            userTransaction.begin();
            bean.transactionStatus();
            throw new RuntimeException("Expected an exception");
        } catch (EJBException e) {
            //ignore
        } finally {
            userTransaction.rollback();
        }
    }

    @Test
    public void testRemoteMethodHasMandatory() throws SystemException, NotSupportedException, NamingException {
        final UserTransaction userTransaction = (UserTransaction)new InitialContext().lookup("java:jboss/UserTransaction");
        final TransactionRemote bean = (TransactionRemote) initialContext.lookup("java:module/" + DescriptorBean.class.getSimpleName() + "!" + TransactionRemote.class.getName());
        userTransaction.begin();
        try {
            Assert.assertEquals(Status.STATUS_ACTIVE, bean.transactionStatus());
        } finally {
            userTransaction.rollback();
        }
        try {
            bean.transactionStatus();
            throw new RuntimeException("Expected an exception");
        } catch (EJBTransactionRequiredException e) {
            //ignore
        }
    }


    @Test
    public void testRemoteMethodHasMandatoryNoMethodIntf() throws SystemException, NotSupportedException, NamingException {
        final UserTransaction userTransaction = (UserTransaction)new InitialContext().lookup("java:jboss/UserTransaction");
        final TransactionRemote bean = (TransactionRemote) initialContext.lookup("java:module/" + DescriptorBean.class.getSimpleName() + "!" + TransactionRemote.class.getName());
        userTransaction.begin();
        try {
            Assert.assertEquals(Status.STATUS_ACTIVE, bean.transactionStatus2());
        } finally {
            userTransaction.rollback();
        }
        try {
            bean.transactionStatus2();
            throw new RuntimeException("Expected an exception");
        } catch (EJBTransactionRequiredException e) {
            //ignore
        }
    }
}
