/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.ejb.transaction.descriptor;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRequiredException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

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
