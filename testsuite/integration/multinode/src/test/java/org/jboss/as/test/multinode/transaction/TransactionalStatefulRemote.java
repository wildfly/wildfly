/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.transaction;

import java.rmi.RemoteException;

/**
 * @author Stuart Douglas
 */
public interface TransactionalStatefulRemote {

    int transactionStatus() throws RemoteException;

    Boolean getCommitSucceeded() throws RemoteException;

    boolean isBeforeCompletion() throws RemoteException;

    void resetStatus() throws RemoteException;

    void sameTransaction(boolean first) throws RemoteException;

    void rollbackOnly() throws RemoteException;

    void setRollbackOnlyBeforeCompletion(boolean rollbackOnlyInBeforeCompletion) throws RemoteException;

}
