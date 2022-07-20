package org.jboss.as.test.iiop.basic;

import java.rmi.RemoteException;

import jakarta.ejb.EJBHome;

/**
 * @author Stuart Douglas
 */
public interface IIOPBasicHome extends EJBHome {

    IIOPBasicRemote create() throws RemoteException;

}
