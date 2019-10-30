/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.multinode.transaction.async;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.junit.Assert;

import java.util.Hashtable;

/**
 * Client bean which lookups for a remote bean on other server and do the call.
 *
 * @author Ondrej Chaloupka
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class ClientBean {

    @Resource
    private UserTransaction userTransaction;

    private TransactionalRemote getRemote(Class<?> beanClass) throws NamingException {
        final Hashtable<String,String> props = new Hashtable<String, String>();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new javax.naming.InitialContext(props);
        final TransactionalRemote remote = (TransactionalRemote) context.lookup(String.format("ejb:/%s//%s!%s",
          TransactionPropagationTestCase.SERVER_DEPLOYMENT, beanClass.getSimpleName(), TransactionalRemote.class.getName()));
        return remote;
    }

    public void callToMandatory() throws Exception {
        final TransactionalRemote remote = getRemote(TransactionalMandatory.class);
        userTransaction.begin();
        try {
            remote.transactionStatus().get();
            Assert.fail("Expecting exception being thrown as async call does not provide transaction context");
        } catch (java.util.concurrent.ExecutionException ee) {
            // ignored - bean with transaction attribute mandatory
            // but async call does not provide transactional context thus the exception is expected
        } finally {
            userTransaction.rollback();
        }
    }

    public void callToStatusByRegistry() throws Exception {
        final TransactionalRemote remote = getRemote(TransactionalStatusByRegistry.class);
        userTransaction.begin();
        try {
            Assert.assertEquals("No transaction expected as async call does not pass txn context",
                (Integer) Status.STATUS_NO_TRANSACTION, remote.transactionStatus().get());
        } finally {
            userTransaction.rollback();
        }
    }

    public void callToStatusByTransactionmanager() throws Exception {
        final TransactionalRemote remote = getRemote(TransactionalStatusByManager.class);
        userTransaction.begin();
        try {
            Assert.assertEquals("No transaction expected as async call does not pass txn context",
                    (Integer) Status.STATUS_NO_TRANSACTION, remote.transactionStatus().get());
        } finally {
            userTransaction.rollback();
        }
    }
}
