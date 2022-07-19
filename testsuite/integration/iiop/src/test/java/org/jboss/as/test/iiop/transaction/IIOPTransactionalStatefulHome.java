package org.jboss.as.test.iiop.transaction;

import java.rmi.RemoteException;

import jakarta.ejb.EJBHome;

/**
 * @author Stuart Douglas
 */
public interface IIOPTransactionalStatefulHome extends EJBHome {

    IIOPTransactionalStatefulRemote create() throws RemoteException;

}
