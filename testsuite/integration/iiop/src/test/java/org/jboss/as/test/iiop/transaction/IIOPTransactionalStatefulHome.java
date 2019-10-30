package org.jboss.as.test.iiop.transaction;

import java.rmi.RemoteException;

import javax.ejb.EJBHome;

/**
 * @author Stuart Douglas
 */
public interface IIOPTransactionalStatefulHome extends EJBHome {

    IIOPTransactionalStatefulRemote create() throws RemoteException;

}
