package org.jboss.as.test.iiop.security;

import java.rmi.RemoteException;

import javax.ejb.EJBHome;

/**
 * @author Stuart Douglas
 */
public interface IIOPSecurityStatelessHome extends EJBHome {

    public IIOPSecurityStatelessRemote create() throws RemoteException;

}
