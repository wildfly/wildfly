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

import java.rmi.RemoteException;
import java.util.concurrent.Future;
import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionManager;
import javax.transaction.SystemException;

/**
 * Asynchronously invoked bean where we expect that transaction manager returns
 * no active status for "current" transaction as propagation should not occur.
 *
 * @author Ondrej Chaloupka
 */
@Stateless
public class TransactionalStatusByManager implements TransactionalRemote {

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager txnManager;

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Future<Integer> transactionStatus() throws RemoteException {
        try {
            return new AsyncResult<Integer>(txnManager.getStatus());
        } catch (SystemException se) {
            throw new RemoteException("Can't get transaction status", se);
        }
    }

}
