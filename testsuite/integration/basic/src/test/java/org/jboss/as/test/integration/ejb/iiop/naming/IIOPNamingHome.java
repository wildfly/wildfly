package org.jboss.as.test.integration.ejb.iiop.naming;

import java.rmi.RemoteException;

import javax.ejb.EJBHome;

/**
 * @author Stuart Douglas
 */
public interface IIOPNamingHome extends EJBHome {

    public IIOPRemote create() throws RemoteException;

}
