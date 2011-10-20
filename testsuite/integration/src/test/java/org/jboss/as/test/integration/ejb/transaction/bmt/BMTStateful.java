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

import org.junit.Assert;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

/**
 * Stateful session bean that uses the same transaction over two method invocations
 *
 * @author Stuart Douglas
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class BMTStateful {

    @Resource
    private EJBContext ejbContext;

    public void createTransaction() {
        try {
            final UserTransaction userTransaction = ejbContext.getUserTransaction();
            Assert.assertEquals(Status.STATUS_NO_TRANSACTION, userTransaction.getStatus());
            userTransaction.begin();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        } catch (NotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollbackTransaction() {
        try {
            final UserTransaction userTransaction = ejbContext.getUserTransaction();
            Assert.assertEquals(Status.STATUS_ACTIVE, userTransaction.getStatus());
            userTransaction.rollback();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }
}
