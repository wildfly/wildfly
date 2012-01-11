package org.jboss.as.test.iiop.transaction;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * @author Stuart Douglas
 */
public interface IIOPTransactionalStatefulRemote extends EJBObject {

    int transactionStatus() throws RemoteException;

    public Boolean getCommitSuceeded() throws RemoteException;

    public boolean isBeforeCompletion() throws RemoteException;

    public void resetStatus() throws RemoteException;

    public void sameTransaction(boolean first) throws RemoteException;

    void rollbackOnly() throws RemoteException;

    public void setRollbackOnlyBeforeCompletion(boolean rollbackOnlyInBeforeCompletion) throws RemoteException;

}
