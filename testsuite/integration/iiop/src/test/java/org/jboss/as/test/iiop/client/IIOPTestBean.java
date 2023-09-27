/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
