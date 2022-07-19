package org.jboss.as.test.iiop.security;

import jakarta.ejb.EJBHome;
import java.rmi.RemoteException;

/**
 * @author Stuart Douglas
 */
public interface IIOPSecurityStatelessHome extends EJBHome {

    IIOPSecurityStatelessRemote create() throws RemoteException;

}
