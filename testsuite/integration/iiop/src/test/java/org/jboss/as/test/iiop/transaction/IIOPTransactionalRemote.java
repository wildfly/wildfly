package org.jboss.as.test.iiop.transaction;

import java.rmi.RemoteException;

import jakarta.ejb.EJBObject;

/**
 * @author Stuart Douglas
 */
public interface IIOPTransactionalRemote extends EJBObject {

    int transactionStatus() throws RemoteException;
}
