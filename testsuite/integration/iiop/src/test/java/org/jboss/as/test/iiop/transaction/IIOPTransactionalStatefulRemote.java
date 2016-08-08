package org.jboss.as.test.iiop.transaction;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * @author Stuart Douglas
 */
public interface IIOPTransactionalStatefulRemote extends EJBObject {

    int transactionStatus() throws RemoteException;

    Boolean getCommitSucceeded() throws RemoteException;

    boolean isBeforeCompletion() throws RemoteException;

    void resetStatus() throws RemoteException;

    void sameTransaction(boolean first) throws RemoteException;

    void rollbackOnly() throws RemoteException;

    void setRollbackOnlyBeforeCompletion(boolean rollbackOnlyInBeforeCompletion) throws RemoteException;

}
