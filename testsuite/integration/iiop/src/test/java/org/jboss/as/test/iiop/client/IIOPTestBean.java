/*
* JBoss, Home of Professional Open Source.
* Copyright 2013, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.iiop.client;

import java.rmi.RemoteException;
import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.TransactionSynchronizationRegistry;

@Remote(IIOPTestRemote.class)
@RemoteHome(IIOPTestBeanHome.class)
@Stateless
public class IIOPTestBean {

    @Resource
    private SessionContext sessionContext;

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;


    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public String callMandatory() throws RemoteException {
        return "transaction-attribute-mandatory";
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public String callNever() throws RemoteException {
        return "transaction-attributte-never";
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public String callRollbackOnly() throws RemoteException {
        sessionContext.setRollbackOnly();
        return "transaction-rollback-only";
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int transactionStatus() throws RemoteException {
        return transactionSynchronizationRegistry.getTransactionStatus();
    }

}
