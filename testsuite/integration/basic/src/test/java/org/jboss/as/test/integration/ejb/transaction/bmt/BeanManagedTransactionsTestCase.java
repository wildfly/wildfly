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
package org.jboss.as.test.integration.ejb.transaction.bmt;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

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
