package org.jboss.as.test.iiop.security;

import javax.ejb.EJBHome;
import java.rmi.RemoteException;

/**
 * @author Stuart Douglas
 */
public interface IIOPSecurityStatelessHome extends EJBHome {

    public IIOPSecurityStatelessRemote create() throws RemoteException;

}
